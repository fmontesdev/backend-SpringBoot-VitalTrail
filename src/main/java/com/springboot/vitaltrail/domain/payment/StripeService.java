package com.springboot.vitaltrail.domain.payment;

import com.springboot.vitaltrail.api.payment.CheckoutSessionDto;
import com.springboot.vitaltrail.api.subscription.SubscriptionDto;
import com.stripe.exception.StripeException;
import java.util.Map;

public interface StripeService {
    String createCheckoutSession(final CheckoutSessionDto request) throws StripeException;

    Map<String, Object> retrieveCheckoutSession(final String sessionId) throws StripeException;

    SubscriptionDto cancelSubscription(String customerId) throws StripeException;
}
