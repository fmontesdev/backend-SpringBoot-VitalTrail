package com.springboot.vitaltrail.domain.payment;

import com.springboot.vitaltrail.domain.subscription.SubscriptionEntity;
import com.springboot.vitaltrail.domain.subscription.SubscriptionRepository;
import com.springboot.vitaltrail.domain.user.UserEntity;
import com.springboot.vitaltrail.domain.user.UserRepository;
import com.springboot.vitaltrail.domain.notification.NotificationService;
import com.springboot.vitaltrail.api.notification.NotificationDto;
import com.springboot.vitaltrail.api.notification.WebhookNotificationFactory;
import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {
    private static final Logger logger = LoggerFactory.getLogger(WebhookServiceImpl.class);

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final StripeDataService stripeDataService;
    private final NotificationService notificationService;
    private final WebhookNotificationFactory notificationFactory;

    /**
     * Valida y encola un evento de Stripe para procesamiento de forma asincrónica
     */
    @Override
    public void validateAndQueueEvent(Event event, String test) throws StripeException {
        logger.info("Queuing Stripe event for processing: {}", event.getType());
        processStripeEvent(event, test);
    }

    /**
     * Procesa un evento de Stripe de forma asincrónica
     */
    @Async("webhookTaskExecutor")
    @Transactional
    @Override
    @Retryable(
        value = {StripeException.class, DataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void processStripeEvent(Event event, String test) throws StripeException {
        logger.info("Processing Stripe event asynchronously: {}", event.getType());

        // Deserializar el objeto del evento.
        // getObject() devuelve Optional.empty() si la API version del evento difiere de la del SDK.
        // En ese caso usamos deserializeUnsafe() como fallback para tolerar versiones distintas.
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            try {
                logger.warn("API version mismatch — falling back to deserializeUnsafe() for event: {}", event.getType());
                stripeObject = dataObjectDeserializer.deserializeUnsafe();
            } catch (com.stripe.exception.EventDataObjectDeserializationException ex) {
                logger.error("Failed to deserialize Stripe object for event {}: {}", event.getType(), ex.getMessage());
                throw new AppException(Error.STRIPE_WEBHOOK_ERROR, ex);
            }
        }

        if (stripeObject == null) {
            logger.error("Failed to deserialize Stripe object for event: {}", event.getType());
            throw new AppException(Error.STRIPE_WEBHOOK_ERROR);
        }

        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted((Session) stripeObject, test);
                break;
            case "customer.subscription.updated":
                handleSubscriptionUpdated((com.stripe.model.Subscription) stripeObject, test);
                break;
            case "customer.subscription.deleted":
                handleSubscriptionDeleted((com.stripe.model.Subscription) stripeObject, test);
                break;
            case "invoice.payment_succeeded":
                handleInvoicePaymentSucceeded((Invoice) stripeObject, test);
                break;
            case "invoice.payment_failed":
                handleInvoicePaymentFailed((Invoice) stripeObject, test);
                break;
            default:
                logger.warn("Unhandled event type: {}", event.getType());
        }
    }

    /**
     * Método de recuperación invocado por Spring Retry cuando se agotan todos los reintentos
     * de {@link #processStripeEvent}. Registra el error y puede utilizarse para guardar el
     * evento fallido o enviar una alerta al equipo.
     *
     * @param e     excepción que causó el fallo definitivo
     * @param event evento de Stripe que no pudo procesarse
     */
    @Recover
    public void recoverFromProcessingFailure(Exception e, Event event) {
        logger.error("Failed to process Stripe event after all retries: {}", event.getId(), e);

        //! Guardar en una tabla de eventos fallidos para revisión manual
        // saveFailedEvent(event, e.getMessage());

        //! Notificar al equipo (email, Slack, etc.)
        // notificationService.sendAlert("Fallo en procesamiento de webhook: " + event.getType(), e.getMessage());
    }

    /**
     * Procesa el evento {@code checkout.session.completed}: crea o actualiza la suscripción
     * en base de datos, marca al usuario como premium y envía un email de bienvenida.
     *
     * @param session objeto {@link Session} deserializado del evento de Stripe
     * @param test    identificador de usuario de prueba (nulo o vacío en producción)
     */
    private void handleCheckoutSessionCompleted(Session session, String test) {
        try {
            String clientReferenceId = session.getClientReferenceId();
            String subscriptionId = session.getSubscription();
            String customerId = session.getCustomer();
            String paymentMethodId = null;

            /// Datos de prueba
            if (test != null && !test.isEmpty()) {
                WebhookTestData.TestUser testUser = WebhookTestData.get(test);
                if (testUser != null) {
                    subscriptionId = testUser.subscriptionId;
                    clientReferenceId = testUser.clientReferenceId;
                    customerId = testUser.customerId;
                    paymentMethodId = testUser.paymentMethodId;
                    logger.info("DATOS DE PRUEBA checkout.session.completed - subscriptionId: {} - clientReferenceId: {} - customerId: {}", subscriptionId, clientReferenceId, customerId);
                }
            }

            if (subscriptionId != null && clientReferenceId != null && customerId != null) {
                logger.info("Processing checkout completion for subscriptionId: {} from clientReferenceId: {}", subscriptionId, clientReferenceId);

                Map<String, Object> subscriptionData = retrieveSubscriptionData(subscriptionId);

                UUID idUser = UUID.fromString(clientReferenceId);
                UserEntity user = userRepository.findByIdUser(idUser)
                    .orElseThrow(() -> new AppException(Error.USER_NOT_FOUND));

                saveOrUpdateSubscription(subscriptionData, user, customerId);
                updateUserPremiumStatus(user, true);

                if (test == null || test.isEmpty()) {
                    paymentMethodId = subscriptionData.containsKey("paymentMethodId") ? (String) subscriptionData.get("paymentMethodId") : null;
                }
                updateUserCustomerIdAndPaymentMethodId(user, customerId, paymentMethodId);

                // Enviar email de bienvenida (no crítico — no interrumpe si falla)
                try {
                    notificationService.sendNotification(notificationFactory.buildWelcome(user, subscriptionData));
                } catch (com.springboot.vitaltrail.domain.exception.NotificationException e) {
                    logger.error("Failed to send welcome email for user {}: {}", user.getIdUser(), e.getMessage());
                }
            } else {
                logger.error("Missing subscriptionId or clientReferenceId in session");
            }
        } catch (Exception e) {
            logger.error("Error processing checkout.session.completed: {}", e.getMessage());
            throw new AppException(Error.STRIPE_WEBHOOK_ERROR, e);
        }
    }

    /**
     * Procesa el evento {@code customer.subscription.updated}: actualiza el estado de la
     * suscripción en base de datos, recalcula el estado premium del usuario y envía un email
     * de cancelación de renovación o de reactivación si {@code cancelAtPeriodEnd} cambió.
     *
     * @param subscription objeto {@link com.stripe.model.Subscription} deserializado del evento
     * @param test         identificador de usuario de prueba (nulo o vacío en producción)
     */
    private void handleSubscriptionUpdated(com.stripe.model.Subscription subscription, String test) {
        try {
            String subscriptionId = subscription.getId();
            logger.info("Processing subscription update: {}", subscriptionId);

            /// Datos de prueba
            if (test != null && !test.isEmpty()) {
                WebhookTestData.TestUser testUser = WebhookTestData.get(test);
                if (testUser != null) {
                    subscriptionId = testUser.subscriptionId;
                    logger.info("DATOS DE PRUEBA customer.subscription.updated - subscriptionId: {}", subscriptionId);
                }
            }

            Optional<SubscriptionEntity> maybeSubscription = subscriptionRepository.findBySubscriptionId(subscriptionId);
            if (maybeSubscription.isEmpty()) {
                logger.info("Subscription {} not found in DB, skipping customer.subscription.updated (race condition on creation)", subscriptionId);
                return;
            }
            SubscriptionEntity existingSubscription = maybeSubscription.get();

            // Capturar el valor anterior para detectar cambios en la renovación automática
            Boolean oldCancelAtPeriodEnd = existingSubscription.getCancelAtPeriodEnd();

            // Actualizar campos básicos
            existingSubscription.setStatus(subscription.getStatus());
            existingSubscription.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
            existingSubscription.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
            existingSubscription.setCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());
            existingSubscription.setLastEventType("customer.subscription.updated");

            // Actualizar datos de producto/precio si hay cambios en los items
            if (subscription.getItems() != null && subscription.getItems().getData() != null &&
                !subscription.getItems().getData().isEmpty()) {

                Map<String, Object> subscriptionData = retrieveSubscriptionData(subscriptionId);

                if (subscriptionData.containsKey("priceId")) existingSubscription.setPriceId((String) subscriptionData.get("priceId"));
                if (subscriptionData.containsKey("productId")) existingSubscription.setProductId((String) subscriptionData.get("productId"));
                if (subscriptionData.containsKey("productName")) existingSubscription.setProductName((String) subscriptionData.get("productName"));
                if (subscriptionData.containsKey("billingInterval")) existingSubscription.setBillingInterval((String) subscriptionData.get("billingInterval"));
                if (subscriptionData.containsKey("subscriptionType")) existingSubscription.setSubscriptionType((String) subscriptionData.get("subscriptionType"));
            }

            saveSubscription(existingSubscription);

            // Actualizar isPremium según el status real
            UserEntity user = existingSubscription.getUser();
            boolean isActive = "active".equals(subscription.getStatus()) || "trialing".equals(subscription.getStatus());
            updateUserPremiumStatus(user, isActive);

            // Notificar si cambió cancelAtPeriodEnd (cancelación o reactivación de renovación)
            Boolean newCancelAtPeriodEnd = subscription.getCancelAtPeriodEnd();
            if (!Objects.equals(oldCancelAtPeriodEnd, newCancelAtPeriodEnd)) {
                try {
                    if (Boolean.TRUE.equals(newCancelAtPeriodEnd)) {
                        logger.info("cancelAtPeriodEnd cambió a true para {}: enviando email de cancelación de renovación", subscriptionId);
                        notificationService.sendNotification(notificationFactory.buildCancellationScheduled(user, existingSubscription));
                    } else {
                        logger.info("cancelAtPeriodEnd cambió a false para {}: enviando email de reactivación", subscriptionId);
                        notificationService.sendNotification(notificationFactory.buildReactivation(user, existingSubscription));
                    }
                } catch (com.springboot.vitaltrail.domain.exception.NotificationException e) {
                    logger.error("Failed to send subscription updated notification for subscription {}: {}", subscriptionId, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error processing customer.subscription.updated: {}", e.getMessage());
            throw new AppException(Error.STRIPE_WEBHOOK_ERROR, e);
        }
    }

    /**
     * Procesa el evento {@code customer.subscription.deleted}: marca la suscripción como
     * cancelada, revoca el estado premium del usuario y envía un email de cancelación.
     *
     * @param subscription objeto {@link com.stripe.model.Subscription} deserializado del evento
     * @param test         identificador de usuario de prueba (nulo o vacío en producción)
     */
    private void handleSubscriptionDeleted(com.stripe.model.Subscription subscription, String test) {
        try {
            String subscriptionId = subscription.getId();
            logger.info("Processing subscription deletion: {}", subscriptionId);

            /// Datos de prueba
            if (test != null && !test.isEmpty()) {
                WebhookTestData.TestUser testUser = WebhookTestData.get(test);
                if (testUser != null) {
                    subscriptionId = testUser.subscriptionId;
                    logger.info("DATOS DE PRUEBA customer.subscription.deleted - subscriptionId: {}", subscriptionId);
                }
            }

            SubscriptionEntity existingSubscription = findSubscriptionOrThrow(subscriptionId);

            existingSubscription.setStatus("canceled");
            existingSubscription.setLastEventType("customer.subscription.deleted");
            existingSubscription.setCancelAtPeriodEnd(true);

            if (subscription.getMetadata() != null &&
                subscription.getMetadata().containsKey("cancellation_reason")) {
                existingSubscription.setCancellationReason(subscription.getMetadata().get("cancellation_reason"));
            }

            saveSubscription(existingSubscription);

            UserEntity user = existingSubscription.getUser();
            updateUserPremiumStatus(user, false);

            // Enviar email de cancelación (no crítico — no interrumpe si falla)
            try {
                notificationService.sendNotification(notificationFactory.buildSubscriptionCancelled(user, existingSubscription));
            } catch (com.springboot.vitaltrail.domain.exception.NotificationException e) {
                logger.error("Failed to send cancellation email for subscription {}: {}", existingSubscription.getSubscriptionId(), e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error processing customer.subscription.deleted: {}", e.getMessage());
            throw new AppException(Error.STRIPE_WEBHOOK_ERROR, e);
        }
    }

    /**
     * Procesa el evento {@code invoice.payment_succeeded}: envía un email de confirmación de
     * renovación únicamente cuando el motivo de facturación es {@code subscription_cycle}.
     *
     * @param invoice objeto {@link Invoice} deserializado del evento de Stripe
     * @param test    identificador de usuario de prueba (nulo o vacío en producción)
     */
    private void handleInvoicePaymentSucceeded(Invoice invoice, String test) {
        try {
            String subscriptionId = invoice.getSubscription();

            /// Datos de prueba
            if (test != null && !test.isEmpty()) {
                WebhookTestData.TestUser testUser = WebhookTestData.get(test);
                if (testUser != null) {
                    subscriptionId = testUser.subscriptionId;
                    logger.info("DATOS DE PRUEBA invoice.payment_succeeded - subscriptionId: {}", subscriptionId);
                }
            }

            if (subscriptionId != null) {
                String billingReason = invoice.getBillingReason();
                logger.info("Processing invoice.payment_succeeded for subscription: {}, billing_reason: {}", subscriptionId, billingReason);

                // Solo enviar email de renovación en ciclos de renovación
                if ("subscription_cycle".equals(billingReason)) {
                    Optional<SubscriptionEntity> maybeSubscription = subscriptionRepository.findBySubscriptionId(subscriptionId);
                    if (maybeSubscription.isEmpty()) {
                        logger.info("Subscription {} not found in DB, skipping renewal email", subscriptionId);
                        return;
                    }
                    SubscriptionEntity subscription = maybeSubscription.get();
                    UserEntity user = subscription.getUser();
                    Map<String, Object> subscriptionData = retrieveSubscriptionData(subscriptionId);
                    try {
                        notificationService.sendNotification(notificationFactory.buildRenewal(user, subscriptionData));
                    } catch (com.springboot.vitaltrail.domain.exception.NotificationException e) {
                        logger.error("Failed to send renewal email for subscription {}: {}", subscriptionId, e.getMessage());
                    }
                } else {
                    logger.info("Skipping renewal email for billing_reason: {} (subscription {})", billingReason, subscriptionId);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing invoice.payment_succeeded: {}", e.getMessage());
            throw new AppException(Error.STRIPE_WEBHOOK_ERROR, e);
        }
    }

    /**
     * Procesa el evento {@code invoice.payment_failed}: marca la suscripción como
     * {@code past_due}, revoca el estado premium del usuario y envía un email de aviso
     * de fallo de pago.
     *
     * @param invoice objeto {@link Invoice} deserializado del evento de Stripe
     * @param test    identificador de usuario de prueba (nulo o vacío en producción)
     */
    private void handleInvoicePaymentFailed(Invoice invoice, String test) {
        try {
            String subscriptionId = invoice.getSubscription();

            /// Datos de prueba
            if (test != null && !test.isEmpty()) {
                WebhookTestData.TestUser testUser = WebhookTestData.get(test);
                if (testUser != null) {
                    subscriptionId = testUser.subscriptionId;
                    logger.info("DATOS DE PRUEBA invoice.payment_failed - subscriptionId: {}", subscriptionId);
                }
            }

            if (subscriptionId != null) {
                logger.info("Processing failed payment for subscription: {}", subscriptionId);

                SubscriptionEntity subscription = findSubscriptionOrThrow(subscriptionId);

                subscription.setStatus("past_due");
                subscription.setLastEventType("invoice.payment_failed");
                saveSubscription(subscription);

                UserEntity user = subscription.getUser();
                updateUserPremiumStatus(user, false);

                // Enviar email de aviso de fallo de pago (no crítico — no interrumpe si falla)
                try {
                    notificationService.sendNotification(notificationFactory.buildPaymentFailed(user, subscription));
                } catch (com.springboot.vitaltrail.domain.exception.NotificationException e) {
                    logger.error("Failed to send payment failed email for subscription {}: {}", subscriptionId, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error processing invoice.payment_failed: {}", e.getMessage());
            throw new AppException(Error.STRIPE_WEBHOOK_ERROR, e);
        }
    }

    /**
     * Obtiene los datos de una suscripción de Stripe (precio, producto, período, etc.)
     * mediante {@link StripeDataService}.
     *
     * @param subscriptionId identificador de la suscripción en Stripe
     * @return mapa con los datos de la suscripción
     * @throws StripeException si la llamada a la API de Stripe falla
     */
    private Map<String, Object> retrieveSubscriptionData(String subscriptionId) throws StripeException {
        return stripeDataService.getSubscriptionData(subscriptionId);
    }

    /**
     * Crea o actualiza una {@link SubscriptionEntity} a partir de los datos de Stripe.
     * Si ya existe una suscripción con el mismo {@code subscriptionId} la actualiza;
     * si no, la crea vinculada al usuario indicado.
     *
     * @param subscriptionData mapa con los datos de la suscripción obtenidos de Stripe
     * @param user             entidad del usuario propietario de la suscripción
     * @param customerId       identificador del cliente en Stripe
     */
    private void saveOrUpdateSubscription(Map<String, Object> subscriptionData, UserEntity user, String customerId) {
        String subscriptionId = (String) subscriptionData.get("subscriptionId");

        Optional<SubscriptionEntity> existingSubscription = subscriptionRepository.findBySubscriptionId(subscriptionId);

        SubscriptionEntity subscription;
        if (existingSubscription.isPresent()) {
            subscription = existingSubscription.get();
        } else {
            subscription = new SubscriptionEntity();
            subscription.setSubscriptionId(subscriptionId);
            subscription.setUser(user);
            subscription.setCustomerId(customerId);
        }

        subscription.setSubscriptionType((String) subscriptionData.get("subscriptionType"));
        subscription.setBillingInterval((String) subscriptionData.get("billingInterval"));
        subscription.setProductId((String) subscriptionData.get("productId"));
        subscription.setProductName((String) subscriptionData.get("productName"));
        subscription.setPriceId((String) subscriptionData.get("priceId"));
        subscription.setCurrentPeriodStart((Long) subscriptionData.get("currentPeriodStart"));
        subscription.setCurrentPeriodEnd((Long) subscriptionData.get("currentPeriodEnd"));
        subscription.setCancelAtPeriodEnd((Boolean) subscriptionData.get("cancelAtPeriodEnd"));
        subscription.setCancellationReason((String) subscriptionData.get("cancellationReason"));
        subscription.setStatus((String) subscriptionData.get("subscriptionStatus"));
        subscription.setLastEventType("checkout.session.completed");
        subscription.setPaymentMethodId((String) subscriptionData.get("paymentMethodId"));
        subscription.setPaymentMethodType((String) subscriptionData.get("paymentMethodType"));
        subscription.setCardBrand((String) subscriptionData.get("cardBrand"));
        subscription.setCardLast4((String) subscriptionData.get("cardLast4"));
        subscription.setCardExpMonth((Long) subscriptionData.get("cardExpMonth"));
        subscription.setCardExpYear((Long) subscriptionData.get("cardExpYear"));

        saveSubscription(subscription);
    }

    /**
     * Busca una suscripción por su {@code subscriptionId} o lanza {@link AppException}
     * con {@link Error#SUBSCRIPTION_NOT_FOUND} si no existe.
     *
     * @param subscriptionId identificador de la suscripción en Stripe
     * @return la entidad de suscripción encontrada
     */
    private SubscriptionEntity findSubscriptionOrThrow(String subscriptionId) {
        return subscriptionRepository.findBySubscriptionId(subscriptionId)
            .orElseThrow(() -> new AppException(Error.SUBSCRIPTION_NOT_FOUND));
    }

    /**
     * Persiste la suscripción en base de datos y registra un log informativo.
     *
     * @param subscription entidad de suscripción a guardar o actualizar
     */
    private void saveSubscription(SubscriptionEntity subscription) {
        subscriptionRepository.save(subscription);
        logger.info("Saved or Updated subscription: {}", subscription.getSubscriptionId());
    }

    /**
     * Actualiza el campo {@code isPremium} del usuario solo si su valor ha cambiado,
     * evitando escrituras innecesarias en base de datos.
     *
     * @param user      entidad del usuario a actualizar
     * @param isPremium nuevo valor del estado premium
     */
    private void updateUserPremiumStatus(UserEntity user, boolean isPremium) {
        if (user.getIsPremium() != isPremium) {
            user.setIsPremium(isPremium);
            userRepository.save(user);
            logger.info("Updated premium status to {} for client {}", isPremium, user.getIdUser());
        }
    }

    /**
     * Almacena el {@code customerId} y el {@code paymentMethodId} de Stripe en el perfil
     * de cliente del usuario, solo si aún no están establecidos.
     *
     * @param user            entidad del usuario a actualizar
     * @param customerId      identificador del cliente en Stripe
     * @param paymentMethodId identificador del método de pago en Stripe
     */
    private void updateUserCustomerIdAndPaymentMethodId(UserEntity user, String customerId, String paymentMethodId) {
        if (user.getClient().getCustomerId() == null || user.getClient().getCustomerId().isEmpty()) {
            user.getClient().setCustomerId(customerId);
        }
        if (user.getClient().getPaymentMethodId() == null || user.getClient().getPaymentMethodId().isEmpty()) {
            user.getClient().setPaymentMethodId(paymentMethodId);
        }
        userRepository.save(user);
        logger.info("Updated stripe customerId {}, paymentMethodId {} for client {}", customerId, paymentMethodId, user.getIdUser());
    }
}
