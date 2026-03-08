package com.springboot.vitaltrail.api.notification;

import com.springboot.vitaltrail.domain.user.UserEntity;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationAssembler {
    public NotificationDto buildNotification(
        String to,
        String subject,
        String template,
        UserEntity user,
        Map<String, Object> subscriptionData,
        String error
    ) {
        NotificationDto.DataSubscription data = NotificationDto.DataSubscription.builder()
            .name(user.getName())
            .surname(user.getSurname())
            .date(LocalDate.now().toString())
            .subscriptionId((String) subscriptionData.get("subscriptionId"))
            .subscriptionType((String) subscriptionData.get("subscriptionType"))
            .productName((String) subscriptionData.get("productName"))
            .priceAmount((Long) subscriptionData.get("priceAmount"))
            .status((String) subscriptionData.get("subscriptionStatus"))
            .currentPeriodStart((Long) subscriptionData.get("currentPeriodStart"))
            .currentPeriodEnd((Long) subscriptionData.get("currentPeriodEnd"))
            .cancelAtPeriodEnd((Boolean) subscriptionData.get("cancelAtPeriodEnd"))
            .cancellationReason((String) subscriptionData.get("cancellationReason"))
            .paymentMethodType((String) subscriptionData.get("paymentMethodType"))
            .cardBrand((String) subscriptionData.get("cardBrand"))
            .cardLast4((String) subscriptionData.get("cardLast4"))
            .cardExpMonth((Long) subscriptionData.get("cardExpMonth"))
            .cardExpYear((Long) subscriptionData.get("cardExpYear"))
            .error(error)
            .build();
        
        return NotificationDto.builder()
            .to(to)
            .subject(subject)
            .template(template)
            .dataSubscription(data)
            .build();
    }
}
