package com.springboot.vitaltrail.api.subscription;

import com.springboot.vitaltrail.api.security.authorization.CheckSecurity;
import com.springboot.vitaltrail.domain.subscription.SubscriptionService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    /**
     * Obtener información de la suscripción de un cliente.
     * Solo accesible por el propio propietario o un usuario ADMIN.
     *
     * @param customerId ID del cliente
     * @return Información de la suscripción
     */
    @CheckSecurity.Protected.isAdminOrOwner
    @GetMapping("/subscription/{customerId}")
    public SubscriptionDto getSubscription(@PathVariable String customerId) {
            return subscriptionService.getActiveSubscriptionByCustomerId(customerId, "active");
    }
}
