package com.springboot.vitaltrail.domain.subscription;

import com.springboot.vitaltrail.api.subscription.AdminSubscriptionStatsDto;
import com.springboot.vitaltrail.api.subscription.AdminSubscriptionsPageDto;
import com.springboot.vitaltrail.api.subscription.SubscriptionDto;

public interface SubscriptionService {
    SubscriptionDto getActiveSubscriptionByCustomerId(String customerId, String status);

    SubscriptionEntity getSubscriptionBySubscriptionId(String subscriptionId);

    SubscriptionDto cancelSubscription(SubscriptionEntity subscriptionEntity);

    AdminSubscriptionStatsDto getAdminSubscriptionStats();

    AdminSubscriptionsPageDto getAdminSubscriptionsPage(int page, int size, String status);
}
