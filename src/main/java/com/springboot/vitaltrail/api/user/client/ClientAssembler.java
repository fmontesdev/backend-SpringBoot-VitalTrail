package com.springboot.vitaltrail.api.user.client;

import com.springboot.vitaltrail.domain.user.client.ClientEntity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientAssembler {
    public ClientDto toClientResponse(ClientEntity clientEntity) {
        return ClientDto.builder()
            .idClient(clientEntity.getIdClient())
            .user(clientEntity.getUser().getIdUser())
            .phone(clientEntity.getPhone())
            .customerId(clientEntity.getCustomerId())
            .paymentMethodId(clientEntity.getPaymentMethodId())
            .build();
    }
}
