package com.springboot.vitaltrail.domain.payment;

import com.springboot.vitaltrail.domain.subscription.SubscriptionEntity;
import com.springboot.vitaltrail.api.subscription.SubscriptionDto;
import com.springboot.vitaltrail.domain.subscription.SubscriptionService;
import com.springboot.vitaltrail.domain.user.UserEntity;
import com.springboot.vitaltrail.domain.user.UserService;
import com.springboot.vitaltrail.api.payment.CheckoutSessionDto;
import com.springboot.vitaltrail.api.security.AuthUtils;
import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionCollection;
import com.stripe.param.SubscriptionListParams;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class StripeServiceImpl implements StripeService {
    private static final Logger logger = LoggerFactory.getLogger(StripeServiceImpl.class);

    private final AuthUtils authUtils;
    private final StripeDataService stripeDataService;
    private final SubscriptionService subscriptionService;
    private final UserService userService;

    /**
     * Crea una sesión de checkout en Stripe
     * 
     * @param request Datos de la sesión de checkout
     * @return ID de la sesión de checkout
     * @throws StripeException Si ocurre un error al crear la sesión
     */
    @Override
    public String createCheckoutSession(CheckoutSessionDto request) throws StripeException {
        SessionCreateParams.Builder params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setSuccessUrl(request.getSuccessUrl())
            .setCancelUrl(request.getCancelUrl())
            .addLineItem(SessionCreateParams.LineItem.builder()
                .setPrice(request.getPriceId())
                .setQuantity(1L)
                .build())
            .setClientReferenceId(authUtils.getCurrentUserId().toString()) // Guardar el ID del usuario
            .setAutomaticTax(SessionCreateParams.AutomaticTax.builder()
                .setEnabled(true)
                .build())
            .setSavedPaymentMethodOptions(SessionCreateParams.SavedPaymentMethodOptions.builder()
                .setPaymentMethodSave(
                    SessionCreateParams.SavedPaymentMethodOptions.PaymentMethodSave.ENABLED
                )
            .build());

        // Si hay un customerId, usarlo
        if (request.getCustomerData() != null && request.getCustomerData().getCustomerId() != null) {
            params.setCustomer(request.getCustomerData().getCustomerId());
        } else if (request.getCustomerData() != null) {
            // Si no hay customerId pero sí hay email, pre-rellenar el email
            params.setCustomerEmail(request.getCustomerData().getEmail());
        }

        Session session = Session.create(params.build());
        return session.getId();
    }

    /**
     * Recupera los datos de una sesión de checkout en Stripe
     * 
     * @param sessionId ID de la sesión de checkout
     * @return Mapa con los datos de la sesión
     * @throws StripeException Si ocurre un error al recuperar los datos
     */
    @Override
    public Map<String, Object> retrieveCheckoutSession(String sessionId) throws StripeException {
        return stripeDataService.getCheckoutSessionData(sessionId);
    }

    /**
     * Cancela la suscripción del usuario actual
     * 
     * @param customerId ID del cliente
     * @return Dto con información de la suscripción cancelada
     * @throws StripeException Si ocurre un error con Stripe
     */
    @Override
    public SubscriptionDto cancelSubscription(String customerId) throws StripeException {
        // Si el usuario no tiene customerId, no tiene suscripción
        if (customerId == null) {
            throw new AppException(Error.SUBSCRIPTION_NOT_FOUND);
        }
        
        // Obtiene todas las suscripciones del cliente desde Stripe
        SubscriptionListParams params = SubscriptionListParams.builder()
            .setCustomer(customerId)
            .setStatus(SubscriptionListParams.Status.ACTIVE)
            .setLimit(1L)
            .build();
        SubscriptionCollection subscriptions = Subscription.list(params);
        
        // Si no hay suscripciones activas
        if (subscriptions.getData().isEmpty()) {
            throw new AppException(Error.SUBSCRIPTION_NOT_FOUND);
        }

        // Cancela inmediatamente la primera suscripción activa desde Stripe
        Subscription subscription = subscriptions.getData().get(0);
        subscription.cancel();

        // Busca la entidad de suscripción del usuario en BD
        SubscriptionEntity subscriptionEntity = subscriptionService.getSubscriptionBySubscriptionId(subscription.getId());
        if (subscriptionEntity == null) {
            throw new AppException(Error.SUBSCRIPTION_NOT_FOUND);
        }

        // Actualizar la entidad de suscripción del usuario en BD
        SubscriptionDto canceledSubscription = subscriptionService.cancelSubscription(subscriptionEntity);

        // Buscar usuario por UUID
        UUID idUser = canceledSubscription.getUser();
        UserEntity user = userService.getUserByIdUser(idUser);

        // Actualizar isPremium del usuario
        if (user.getIsPremium() != false) {
            userService.saveIsPremium(user, false);
            logger.info("Updated premium status to false for client {}", user.getIdUser());
        }
        
        return canceledSubscription;
    }
}
