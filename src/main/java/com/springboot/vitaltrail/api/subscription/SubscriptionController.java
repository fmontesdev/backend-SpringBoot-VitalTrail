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

    /**
     * Obtener estadísticas de suscripciones para el panel de administración.
     * Solo accesible por ROLE_ADMIN.
     *
     * @return Dto con contadores y churn rate del mes en curso
     */
    @CheckSecurity.Protected.isAdmin
    @GetMapping("/admin/subscriptions/stats")
    public AdminSubscriptionStatsDto getAdminSubscriptionStats() {
        return subscriptionService.getAdminSubscriptionStats();
    }

    /**
     * Obtener lista paginada de suscripciones para el panel de administración.
     * Solo accesible por ROLE_ADMIN.
     *
     * @param page   Número de página (0-based), por defecto 0
     * @param size   Tamaño de página, por defecto 20
     * @param status Filtro de estado (opcional): "active", "canceled", etc.
     * @return Dto con la lista paginada y metadatos de paginación
     */
    @CheckSecurity.Protected.isAdmin
    @GetMapping("/admin/subscriptions")
    public AdminSubscriptionsPageDto getAdminSubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return subscriptionService.getAdminSubscriptionsPage(page, size, status);
    }
}
