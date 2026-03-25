package com.springboot.vitaltrail.domain.payment;

import com.springboot.vitaltrail.api.payment.CheckoutSessionDto;
import com.springboot.vitaltrail.api.subscription.SubscriptionDto;
import java.util.Map;

public interface StripeService {
    String createCheckoutSession(final CheckoutSessionDto request);

    Map<String, Object> retrieveCheckoutSession(final String sessionId);

    SubscriptionDto cancelSubscription(String customerId);

    SubscriptionDto cancelAtPeriodEnd(String customerId);

    SubscriptionDto reactivateSubscription(String customerId);
}
