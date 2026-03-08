package com.springboot.vitaltrail.api.notification;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import jakarta.annotation.Nullable;
// import io.micrometer.common.lang.Nullable;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class NotificationDto {
    @NotNull
    private String to;
    @NotNull
    private String subject;
    @NotNull
    private String template;
    @NotNull
    private DataSubscription dataSubscription;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataSubscription {
        private String name;
        private String surname;
        private String date;
        private String subscriptionId;
        private String subscriptionType;
        private String productName;
        private Long priceAmount;
        private String status;
        private Long currentPeriodStart;
        private Long currentPeriodEnd;
        private Boolean cancelAtPeriodEnd;
        private String cancellationReason;
        private String paymentMethodType;
        private String cardBrand;
        private String cardLast4;
        private Long cardExpMonth;
        private Long cardExpYear;
        @Nullable
        private String error;
    }
}
