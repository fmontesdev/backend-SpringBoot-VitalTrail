package com.springboot.vitaltrail.api.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.springboot.vitaltrail.domain.subscription.SubscriptionEntity;

@Component
@RequiredArgsConstructor
public class SubscriptionAssembler {
    public SubscriptionDto toSubscriptionResponse(SubscriptionEntity entity) {
        return buildSubscription(entity);
    }

    private SubscriptionDto buildSubscription(SubscriptionEntity entity) {
        SubscriptionDto.SubscriptionDtoBuilder builder = SubscriptionDto.builder()
            .subscriptionId(entity.getSubscriptionId())
            .subscriptionType(entity.getSubscriptionType())
            .billingInterval(entity.getBillingInterval())
            .user(entity.getUser().getIdUser())
            .customerId(entity.getCustomerId())
            .productId(entity.getProductId())
            .productName(entity.getProductName())
            .priceId(entity.getPriceId())
            .paymentMethodId(entity.getPaymentMethodId())
            .paymentMethodType(entity.getPaymentMethodType())
            .cardBrand(entity.getCardBrand())
            .cardLast4(entity.getCardLast4())
            .cardExpMonth(entity.getCardExpMonth())
            .cardExpYear(entity.getCardExpYear())
            .currentPeriodStart(entity.getCurrentPeriodStart())
            .currentPeriodEnd(entity.getCurrentPeriodEnd())
            .cancelAtPeriodEnd(entity.getCancelAtPeriodEnd())
            .cancellationReason(entity.getCancellationReason())
            .status(entity.getStatus())
            .lastEventType(entity.getLastEventType())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt());

        return builder.build();
    }
}
