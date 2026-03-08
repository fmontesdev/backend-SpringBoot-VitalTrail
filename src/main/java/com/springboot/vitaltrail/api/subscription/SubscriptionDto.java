package com.springboot.vitaltrail.api.subscription;

import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SubscriptionDto {
    private String subscriptionId;
    private String subscriptionType;
    private String billingInterval;
    private UUID user;
    private String customerId;
    private String productId;
    private String productName;
    private String priceId;
    private String paymentMethodId;
    private String paymentMethodType;
    private String cardBrand;
    private String cardLast4;
    private Long cardExpMonth;
    private Long cardExpYear;
    private Long currentPeriodStart;
    private Long currentPeriodEnd;
    private Boolean cancelAtPeriodEnd;
    private String cancellationReason;
    private String status;
    private String lastEventType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
