package com.springboot.vitaltrail.api.subscription;

import com.springboot.vitaltrail.domain.subscription.SubscriptionService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;
    
    /**
     * Obtener información de la suscripción de un cliente
     * @param customerId ID del cliente
     * @return Información de la suscripción
     */
    @GetMapping("/subscription/{customerId}")
    public SubscriptionDto getSubscription(@PathVariable String customerId) {
            // Recuperar la información de la suscripción
            return subscriptionService.getActiveSubscriptionByCustomerId(customerId, "active");
    }
}
