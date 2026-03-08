package com.springboot.vitaltrail.api.payment;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class CheckoutSessionDto {
    private String priceId;
    private String successUrl;
    private String cancelUrl;
    private CustomerDataDto customerData;
}
