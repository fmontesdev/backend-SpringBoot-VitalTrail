package com.springboot.vitaltrail.api.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminSubscriptionItemDto {
    private String subscriptionId;
    private String status;
    private String billingInterval;
    private String productName;
    private Long currentPeriodEnd;
    private Boolean cancelAtPeriodEnd;
    private String customerId;
    private AdminSubscriptionUserDto user;
}
