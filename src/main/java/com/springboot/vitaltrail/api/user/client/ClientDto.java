package com.springboot.vitaltrail.api.user.client;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ClientDto {
    private Long idClient;
    @NotNull
    private UUID user;
    private String phone;
    private String customerId;
    private String paymentMethodId;
}
