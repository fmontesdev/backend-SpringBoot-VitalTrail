package com.springboot.vitaltrail.api.notification;

import com.springboot.vitaltrail.domain.subscription.SubscriptionEntity;
import com.springboot.vitaltrail.domain.user.UserEntity;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebhookNotificationFactory {

    private static final String ERROR_MSG = "No se pudo conectar con el servicio Mailgun";

    private final NotificationAssembler notificationAssembler;

    public NotificationDto buildWelcome(UserEntity user, Map<String, Object> subscriptionData) {
        return notificationAssembler.buildNotification(
            user.getEmail(),
            "VitalTrail Premium — Bienvenido",
            "welcome-email",
            user, subscriptionData, ERROR_MSG
        );
    }

    public NotificationDto buildRenewal(UserEntity user, Map<String, Object> subscriptionData) {
        return notificationAssembler.buildNotification(
            user.getEmail(),
            "VitalTrail Premium — Confirmación de renovación",
            "renewal-email",
            user, subscriptionData, ERROR_MSG
        );
    }

    public NotificationDto buildCancellationScheduled(UserEntity user, SubscriptionEntity subscription) {
        return notificationAssembler.buildNotification(
            user.getEmail(),
            "VitalTrail Premium — Cancelación de renovación automática",
            "cancellation-scheduled-email",
            user, subscription, ERROR_MSG
        );
    }

    public NotificationDto buildReactivation(UserEntity user, SubscriptionEntity subscription) {
        return notificationAssembler.buildNotification(
            user.getEmail(),
            "VitalTrail Premium — Reactivación de suscripción",
            "reactivation-email",
            user, subscription, ERROR_MSG
        );
    }

    public NotificationDto buildSubscriptionCancelled(UserEntity user, SubscriptionEntity subscription) {
        return notificationAssembler.buildNotification(
            user.getEmail(),
            "VitalTrail Premium — Tu suscripción ha sido cancelada",
            "subscription-cancelled-email",
            user, subscription, ERROR_MSG
        );
    }

    public NotificationDto buildPaymentFailed(UserEntity user, SubscriptionEntity subscription) {
        return notificationAssembler.buildNotification(
            user.getEmail(),
            "VitalTrail Premium — Problema con el pago de tu suscripción",
            "payment-failed-email",
            user, subscription, ERROR_MSG
        );
    }
}
