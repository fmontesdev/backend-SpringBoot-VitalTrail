package com.springboot.vitaltrail.api.payment;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class CustomerDataDto {
    private String email;
    private String customerId;
    // private String paymentMethodId;
}
