package com.springboot.vitaltrail.domain.payment;

import com.springboot.vitaltrail.domain.subscription.SubscriptionEntity;
import com.springboot.vitaltrail.domain.subscription.SubscriptionRepository;
import com.springboot.vitaltrail.domain.user.UserEntity;
import com.springboot.vitaltrail.domain.user.UserRepository;
import com.springboot.vitaltrail.domain.notification.NotificationService;
import com.springboot.vitaltrail.api.notification.NotificationAssembler;
import com.springboot.vitaltrail.api.notification.NotificationDto;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
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
    private final NotificationAssembler notificationAssembler;

    // Datos de prueba centralizados
    private static final Map<String, TestUser> TEST_USERS = new HashMap<>();
    
    static {
        // Usuario Valdes
        TEST_USERS.put("test_valdes", new TestUser(
            "sub_1R5pB7KOmpY53KT5Rq9UtxGo",
            "c45d7b70-7b9a-4ab2-b937-a3755a49d7a0",
            "cus_RvXlkPriIv34F6",
            "pm_1R5Rd5KOmpY53KT5MiQzXtuo"
        ));
        
        // Usuario Llorens
        TEST_USERS.put("test_llorens", new TestUser(
            "sub_1R5mknKOmpY53KT5Q9zgCjlv",
            "b7b67994-abda-4c52-9f58-82c7e346d884",
            "cus_RykKRclTc9yT8G",
            "pm_1R5RjpKOmpY53KT5aRzJBncN"
        ));

        // Usuario Prueba
        TEST_USERS.put("test_prueba", new TestUser(
            "sub_1R5Rr9KOmpY53KT5BWaM2ghg",
            "c8f8a089-13ee-4c5e-a3ef-8ea33144ebab",
            "cus_RzQiRPj1aTA3Lz",
            "pm_1R5Rr7KOmpY53KT5wm4jVA9G"
        ));

        // Usuario framondo
        TEST_USERS.put("test_framondo", new TestUser(
            "sub_1R63ugKOmpY53KT599sU2QLk",
            "d82d33f7-afac-4138-a9a8-092ffa3a37a8",
            "cus_S042MpqByzEgoV",
            "pm_1R63ueKOmpY53KT5CGSCjYjK"
        ));

            // Usuario framondo
            TEST_USERS.put("test_prueba4", new TestUser(
                "sub_1R6GtCKOmpY53KT5yeeFL6xF",
                "fa363c07-fe94-4c01-90a7-72cd970fc7ed",
                "cus_S0HSk9sA2g7qFh",
                "pm_1R6GtAKOmpY53KT5zg6CXytW"
            ));
    }

    /**
     * Clase para datos de usuario de prueba
     */
    static class TestUser {
        final String subscriptionId;
        final String clientReferenceId;
        final String customerId;
        final String paymentMethodId;
        
        TestUser(String subscriptionId, String clientReferenceId, String customerId, String paymentMethodId) {
            this.subscriptionId = subscriptionId;
            this.clientReferenceId = clientReferenceId;
            this.customerId = customerId;
            this.paymentMethodId = paymentMethodId;
        }
    }

    /**
     * Valida y encola un evento de Stripe para procesamiento de forma asincrónica
     * @param event
     */
    @Override
    public void validateAndQueueEvent(Event event, String test) throws StripeException {
        logger.info("Queuing Stripe event for processing: {}", event.getType());
        // Encola el evento para procesamiento asincrónico
        processStripeEvent(event, test);
    }

    /**
     * Procesa un evento de Stripe de forma asincrónica
     * @param event
     * @throws StripeException
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
        
        // Deserializar el objeto del evento
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
        
        if (stripeObject == null) {
            logger.error("Failed to deserialize Stripe object");
            //! Pasar a excepciones centralizadas
            throw new RuntimeException("Failed to deserialize Stripe object");
        }
        
        // Manejar diferentes tipos de eventos
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

    // Método para manejar el caso cuando todos los reintentos fallan
    @Recover
    public void recoverFromProcessingFailure(Exception e, Event event) {
        logger.error("Failed to process Stripe event after all retries: {}", event.getId(), e);
        
        //! Guardar en una tabla de eventos fallidos para revisión manual
        // saveFailedEvent(event, e.getMessage());
        
        //! Notificar al equipo (email, Slack, etc.)
        // notificationService.sendAlert("Fallo en procesamiento de webhook: " + event.getType(), e.getMessage());
    }

    /**
     * Maneja el evento checkout.session.completed
     * @param session
     */
    private void handleCheckoutSessionCompleted(Session session, String test) {
        try {
            String clientReferenceId = session.getClientReferenceId();
            String subscriptionId = session.getSubscription();
            String customerId = session.getCustomer();
            String paymentMethodId = null;

            //! Datos de prueba
            if (test != null && !test.isEmpty()) {
                TestUser testUser = TEST_USERS.get(test);
                if (testUser != null) {
                    subscriptionId = testUser.subscriptionId;
                    clientReferenceId = testUser.clientReferenceId;
                    customerId = testUser.customerId;
                    paymentMethodId = testUser.paymentMethodId;
                    logger.info("DATOS DE PRUEBA checkout.session.completed - subscriptionId: {} - clientReferenceId: {} - customerId: {}", subscriptionId, clientReferenceId, customerId);
                }
            }
            
            if (subscriptionId != null && clientReferenceId != null && customerId != null) {
                logger.info("Processing checkout completion for subscriptionId: " + subscriptionId + " from clientReferenceId: " + clientReferenceId);
                
                // Recuperar datos de la suscripción desde Stripe
                Map<String, Object> subscriptionData = retrieveSubscriptionData(subscriptionId);
                
                // Buscar usuario por ID
                UUID idUser = UUID.fromString(clientReferenceId);
                UserEntity user = userRepository.findByIdUser(idUser)
                    .orElseThrow(() -> new RuntimeException("User not found: " + idUser));
                
                // Guardar o actualizar la suscripción
                saveOrUpdateSubscription(subscriptionData, user, customerId);

                // Actualizar usuario a premium si no lo es
                updateUserPremiumStatus(user, true);

                // Actualizar customerId y paymentMethodId en usuario si no lo tiene
                if (test == null || test.isEmpty()) {
                    paymentMethodId = subscriptionData.containsKey("paymentMethodId") ? (String) subscriptionData.get("paymentMethodId") : null;
                }
                updateUserCustomerIdAndPaymentMethodId(user, customerId, paymentMethodId);

                // Enviar notificación de suscripción por email al usuario
                NotificationDto notification = createSubscriptionNotification(user, subscriptionId, subscriptionData);
                notificationService.sendNotification(notification);
            } else {
                logger.error("Missing subscriptionId or clientReferenceId in session");
            }
        } catch (Exception e) {
            logger.error("Error processing checkout.session.completed: {}", e.getMessage());
            //! Pasar a excepciones centralizadas
            throw new RuntimeException("Failed to process checkout session", e);
        }
    }

    /**
     * Maneja el evento customer.subscription.updated
     * @param subscription
     */
    private void handleSubscriptionUpdated(com.stripe.model.Subscription subscription, String test) {
        try {
            String subscriptionId = subscription.getId();
            logger.info("Processing subscription update: {}", subscriptionId);

            //! Datos de prueba
            if (test != null && !test.isEmpty()) {
                TestUser testUser = TEST_USERS.get(test);
                if (testUser != null) {
                    subscriptionId = testUser.subscriptionId;
                    logger.info("DATOS DE PRUEBA customer.subscription.updated - subscriptionId: {}", subscriptionId);
                }
            }

            // Buscar suscripción o generar error si no existe
            SubscriptionEntity existingSubscription = findSubscriptionOrThrow(subscriptionId);
                
            // Actualizar campos básicos
            existingSubscription.setStatus(subscription.getStatus());
            existingSubscription.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
            existingSubscription.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
            existingSubscription.setCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());
            existingSubscription.setLastEventType("customer.subscription.updated");
            
            // Actualizar elementos de la suscripción si hay cambios
            if (subscription.getItems() != null && subscription.getItems().getData() != null && 
                !subscription.getItems().getData().isEmpty()) {
                
                // Obtener detalles expandidos para actualización completa
                Map<String, Object> subscriptionData = retrieveSubscriptionData(subscriptionId);
                
                // Actualizar datos del producto y precio si han cambiado
                if (subscriptionData.containsKey("priceId")) {
                    existingSubscription.setPriceId((String) subscriptionData.get("priceId"));
                }
                if (subscriptionData.containsKey("productId")) {
                    existingSubscription.setProductId((String) subscriptionData.get("productId"));
                }
                if (subscriptionData.containsKey("productName")) {
                    existingSubscription.setProductName((String) subscriptionData.get("productName"));
                }
                if (subscriptionData.containsKey("billingInterval")) {
                    existingSubscription.setBillingInterval((String) subscriptionData.get("billingInterval"));
                }
                if (subscriptionData.containsKey("subscriptionType")) {
                    existingSubscription.setSubscriptionType((String) subscriptionData.get("subscriptionType"));
                }
            }
            
            // Guardar suscripción
            saveSubscription(existingSubscription);

            // Obtener el usuario asociado y actualizar isPremium
            UserEntity user = existingSubscription.getUser();
            updateUserPremiumStatus(user, true);
        } catch (Exception e) {
            logger.error("Error processing customer.subscription.updated: {}", e.getMessage());
            //! Pasar a excepciones centralizadas
            throw new RuntimeException("Failed to process subscription update", e);
        }
    }

    /**
     * Maneja el evento customer.subscription.deleted
     * @param subscription
     */
    private void handleSubscriptionDeleted(com.stripe.model.Subscription subscription, String test) {
        try {
            String subscriptionId = subscription.getId();
            logger.info("Processing subscription deletion: {}", subscriptionId);

            //! Datos de prueba
            if (test != null && !test.isEmpty()) {
                TestUser testUser = TEST_USERS.get(test);
                if (testUser != null) {
                    subscriptionId = testUser.subscriptionId;
                    logger.info("DATOS DE PRUEBA customer.subscription.deleted - subscriptionId: {}", subscriptionId);
                }
            }
            
            // Buscar suscripción o generar error si no existe
            SubscriptionEntity existingSubscription = findSubscriptionOrThrow(subscriptionId);
            
            // Actualizar suscripción
            existingSubscription.setStatus("canceled");
            existingSubscription.setLastEventType("customer.subscription.deleted");
            existingSubscription.setCancelAtPeriodEnd(true);
            
            // Si hay razón de cancelación en los metadatos, incorporarla
            if (subscription.getMetadata() != null && 
                subscription.getMetadata().containsKey("cancellation_reason")) {
                existingSubscription.setCancellationReason(
                    subscription.getMetadata().get("cancellation_reason"));
            }
            
            // Guardar suscripción
            saveSubscription(existingSubscription);

            // Obtener el usuario asociado y actualizar isPremium
            UserEntity user = existingSubscription.getUser();
            updateUserPremiumStatus(user, false);
        } catch (Exception e) {
            logger.error("Error processing customer.subscription.deleted: {}", e.getMessage());
            //! Pasar a excepciones centralizadas
            throw new RuntimeException("Failed to process subscription delete", e);
        }
    }

    /**
     * Maneja el evento invoice.payment_succeeded
     * @param invoice
     */
    private void handleInvoicePaymentSucceeded(Invoice invoice, String test) {
        try {
            String subscriptionId = invoice.getSubscription();

            //! Datos de prueba
            if (test != null && !test.isEmpty()) {
                TestUser testUser = TEST_USERS.get(test);
                if (testUser != null) {
                    subscriptionId = testUser.subscriptionId;
                    logger.info("DATOS DE PRUEBA invoice.payment_succeeded - subscriptionId: {}", subscriptionId);
                }
            }

            if (subscriptionId != null) {
                logger.info("Processing successful payment for subscription: {}", subscriptionId);
                
                // Buscar suscripción o generar error si no existe
                SubscriptionEntity subscription = findSubscriptionOrThrow(subscriptionId);
                
                // Actualizar suscripción
                subscription.setStatus("active");
                subscription.setLastEventType("invoice.payment_succeeded");
                saveSubscription(subscription);

                // Obtener el usuario asociado y actualizar isPremium
                UserEntity user = subscription.getUser();
                updateUserPremiumStatus(user, true);

                //! Guardar el historial de facturas ???
                //! Enviar email de alerta al usuario ???
            }
        } catch (Exception e) {
            logger.error("Error processing invoice.payment_succeeded: {}", e.getMessage());
            //! Pasar a excepciones centralizadas
            throw new RuntimeException("Failed to process payment succeeded", e);
        }
    }

    /**
     * Maneja el evento invoice.payment_failed
     * @param invoice
     */
    private void handleInvoicePaymentFailed(Invoice invoice, String test) {
        try {
            String subscriptionId = invoice.getSubscription();

            //! Datos de prueba
            if (test != null && !test.isEmpty()) {
                TestUser testUser = TEST_USERS.get(test);
                if (testUser != null) {
                    subscriptionId = testUser.subscriptionId;
                    logger.info("DATOS DE PRUEBA invoice.payment_failed - subscriptionId: {}", subscriptionId);
                }
            }

            if (subscriptionId != null) {
                logger.info("Processing failed payment for subscription: {}", subscriptionId);
                
                // Buscar suscripción o generar error si no existe
                SubscriptionEntity subscription = findSubscriptionOrThrow(subscriptionId);
                
                // Actualizar suscripción
                subscription.setStatus("past_due"); // El estado usual en Stripe para pagos fallidos
                subscription.setLastEventType("invoice.payment_failed");
                saveSubscription(subscription);
                
                // Obtener el usuario asociado y actualizar isPremium
                UserEntity user = subscription.getUser();
                updateUserPremiumStatus(user, false);

                //! Enviar email de alerta al usuario ???
                //! Limitar funcionalidades hasta que el pago se resuelva ???
            }
        } catch (Exception e) {
            logger.error("Error processing invoice.payment_failed: {}", e.getMessage());
            //! Pasar a excepciones centralizadas
            throw new RuntimeException("Failed to process payment failed", e);
        }
    }

    /**
     * Recupera datos detallados de una suscripción desde Stripe
     * @param subscriptionId
     * @return
     */
    private Map<String, Object> retrieveSubscriptionData(String subscriptionId) throws StripeException {
        return stripeDataService.getSubscriptionData(subscriptionId);
    }

    /**
     * Guarda o actualiza una suscripción en la base de datos
     * @param subscriptionData
     * @param user
     * @param customerId
     */
    private void saveOrUpdateSubscription(Map<String, Object> subscriptionData, UserEntity user, String customerId) {
        String subscriptionId = (String) subscriptionData.get("subscriptionId");

        // Buscar si ya existe
        Optional<SubscriptionEntity> existingSubscription = subscriptionRepository.findBySubscriptionId(subscriptionId);
            
        SubscriptionEntity subscription;
        if (existingSubscription.isPresent()) {
            // Actualizar suscripción existente
            subscription = existingSubscription.get();
        } else {
            // Crear nueva suscripción
            subscription = new SubscriptionEntity();
            subscription.setSubscriptionId(subscriptionId);
            subscription.setUser(user);
            subscription.setCustomerId(customerId);
        }
        
        // Actualizar campos
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
        
        // Guardar suscripción
        saveSubscription(subscription);
    }

    /**
     * Busca una suscripción por ID, generando error si no existe
     * @param subscriptionId
     * @return
     */
    private SubscriptionEntity findSubscriptionOrThrow(String subscriptionId) {
        return subscriptionRepository.findBySubscriptionId(subscriptionId)
            .orElseThrow(() -> new AppException(Error.SUBSCRIPTION_NOT_FOUND));
    }

    /**
     * Guarda una suscripción en la base de datos
     * @param subscription
     */
    private void saveSubscription(SubscriptionEntity subscription) {
        subscriptionRepository.save(subscription);
        logger.info("Saved or Updated subscription: {}", subscription.getSubscriptionId());
    }

    /**
     * Actualiza el estado premium de un cliente si es necesario
     * @param user
     * @param isPremium
     */
    private void updateUserPremiumStatus(UserEntity user, boolean isPremium) {
        if (user.getIsPremium() != isPremium) {
            user.setIsPremium(isPremium);
            userRepository.save(user);
            logger.info("Updated premium status to {} for client {}", isPremium, user.getIdUser());
        }
    }

    /**
     * Actualiza el customerId de un cliente si no tiene uno
     * @param user
     * @param customerId
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

    private NotificationDto createSubscriptionNotification(UserEntity user, String subscriptionId, Map<String, Object> subscriptionData) {
        String to = user.getEmail();
        String subject = "Confirmación Suscripción VitalTrail";
        String template = "subscription-email";
        String error = "No se pudo conectar con el servicio Mailgun";

        return notificationAssembler.buildNotification(to, subject, template, user, subscriptionData, error);
    }
}
