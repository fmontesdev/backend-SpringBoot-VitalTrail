package com.springboot.vitaltrail.domain.notification;

import com.springboot.vitaltrail.api.notification.NotificationDto;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.springboot.vitaltrail.domain.exception.NotificationException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final WebClient mailgunWebClient;
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Override
    public void sendNotification(NotificationDto notification) throws NotificationException {
        try {
            mailgunWebClient.post()
                .uri("/notifications/email")
                .bodyValue(notification)
                .retrieve()
                .toEntity(String.class)
                .block();

                logger.info("Sent email notification with subscriptionId {} to {}", notification.getDataSubscription().getSubscriptionId(),  notification.getTo());
                    
        } catch (WebClientResponseException e) {
            logger.error("Error en WebClient: {}", e.getMessage());
            throw new NotificationException(
                "Error al comunicarse con el servidor de Mailgun",
                e.getStatusCode().value(),
                e.getMessage()
            );
        } catch (Exception e) {
            logger.error("Error inesperado al enviar notificación: {}", e.getMessage());
            throw new NotificationException(
                "Error inesperado al enviar notificación",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                e.getMessage()
            );
        }
    }
}
