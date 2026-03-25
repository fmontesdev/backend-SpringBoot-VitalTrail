package com.springboot.vitaltrail.api.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubscriptionActionDto {

    @NotBlank(message = "El customerId no puede estar vacío")
    private String customerId;
}
