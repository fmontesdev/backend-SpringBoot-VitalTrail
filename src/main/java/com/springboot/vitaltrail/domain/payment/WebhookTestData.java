package com.springboot.vitaltrail.domain.payment;

import java.util.HashMap;
import java.util.Map;

public class WebhookTestData {

    private static final Map<String, TestUser> USERS = new HashMap<>();

    static {
        USERS.put("test_valdes", new TestUser(
            "sub_1TDnJrKOmpY53KT5ysuB920m",
            "c45d7b70-7b9a-4ab2-b937-a3755a49d7a0",
            "cus_UCAP1Zln5fWMuO",
            "pm_1TDm4wKOmpY53KT5b8ZptuLH"
        ));
    }

    public static TestUser get(String key) {
        return USERS.get(key);
    }

    public static class TestUser {
        public final String subscriptionId;
        public final String clientReferenceId;
        public final String customerId;
        public final String paymentMethodId;

        TestUser(String subscriptionId, String clientReferenceId, String customerId, String paymentMethodId) {
            this.subscriptionId = subscriptionId;
            this.clientReferenceId = clientReferenceId;
            this.customerId = customerId;
            this.paymentMethodId = paymentMethodId;
        }
    }

    private WebhookTestData() {}
}
