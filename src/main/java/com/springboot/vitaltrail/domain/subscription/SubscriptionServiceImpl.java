package com.springboot.vitaltrail.domain.subscription;

import com.springboot.vitaltrail.api.subscription.AdminSubscriptionItemDto;
import com.springboot.vitaltrail.api.subscription.AdminSubscriptionStatsDto;
import com.springboot.vitaltrail.api.subscription.AdminSubscriptionsPageDto;
import com.springboot.vitaltrail.api.subscription.SubscriptionDto;
import com.springboot.vitaltrail.api.subscription.SubscriptionAssembler;

import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionAssembler subscriptionAssembler;


    /**
     * Obtiene la suscripción del usuario actual
     * 
     * @param customerId ID del cliente
     * @param status Estado de la suscripción
     * @return Dto con información de la suscripción
     */
    @Override
    public SubscriptionDto getActiveSubscriptionByCustomerId(String customerId, String status) {
            // Si el usuario no tiene customerId, no tiene suscripción
            if (customerId == null) {
                throw new AppException(Error.SUBSCRIPTION_NOT_FOUND);
            }

            SubscriptionEntity subscription = subscriptionRepository.findByCustomerIdAndStatus(customerId, status)
                .orElseThrow(() -> new AppException(Error.SUBSCRIPTION_NOT_FOUND));

            return subscriptionAssembler.toSubscriptionResponse(subscription);
    }

    /**
     * Obtiene la suscripción del usuario actual
     * 
     * @param subscriptionId ID de la suscripción
     * @return Dto con información de la suscripción
     */
    @Override
    public SubscriptionEntity getSubscriptionBySubscriptionId(String subscriptionId) {
            if (subscriptionId == null) {
                throw new AppException(Error.SUBSCRIPTION_NOT_FOUND);
            }

            return subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new AppException(Error.SUBSCRIPTION_NOT_FOUND));
    }

    /**
     * Cancela la suscripción de un usuario
     *
     * @param subscriptionEntity Entidad de la suscripción
     * @return Dto con información de la suscripción cancelada
     */
    @Override
    public SubscriptionDto cancelSubscription(SubscriptionEntity subscriptionEntity) {
        subscriptionEntity.setStatus("canceled");
        subscriptionEntity.setCancelAtPeriodEnd(true);
        subscriptionEntity.setLastEventType("customer.subscription.deleted");
        SubscriptionEntity canceledSubscription = subscriptionRepository.save(subscriptionEntity);

        return subscriptionAssembler.toSubscriptionResponse(canceledSubscription);
    }

    /**
     * Devuelve las estadísticas de suscripciones para el panel de administración.
     *
     * @return Dto con contadores y churn rate del mes en curso
     */
    @Override
    @Transactional(readOnly = true)
    public AdminSubscriptionStatsDto getAdminSubscriptionStats() {
        long active = subscriptionRepository.countByStatus("active");
        long monthly = subscriptionRepository.countByStatusAndBillingInterval("active", "month");
        long annual = subscriptionRepository.countByStatusAndBillingInterval("active", "year");
        long newThisMonth = subscriptionRepository.countNewThisMonth();
        long canceledThisMonth = subscriptionRepository.countCanceledThisMonth();

        double churnRate = active == 0
            ? 0.0
            : BigDecimal.valueOf((double) canceledThisMonth / active * 100.0)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        return AdminSubscriptionStatsDto.builder()
            .active(active)
            .monthly(monthly)
            .annual(annual)
            .newThisMonth(newThisMonth)
            .canceledThisMonth(canceledThisMonth)
            .churnRate(churnRate)
            .build();
    }

    /**
     * Devuelve una página de suscripciones, opcionalmente filtrada por estado.
     *
     * @param page   Número de página (0-based)
     * @param size   Tamaño de página
     * @param status Filtro de estado; si es null o vacío, devuelve todos
     * @return Dto con la lista paginada y metadatos de paginación
     */
    @Override
    @Transactional(readOnly = true)
    public AdminSubscriptionsPageDto getAdminSubscriptionsPage(int page, int size, String status) {
        PageRequest pageable = PageRequest.of(page, size);

        Page<SubscriptionEntity> resultPage = (status != null && !status.isBlank())
            ? subscriptionRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
            : subscriptionRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<AdminSubscriptionItemDto> items = resultPage.getContent().stream()
            .map(subscriptionAssembler::toAdminSubscriptionItem)
            .toList();

        return AdminSubscriptionsPageDto.builder()
            .subscriptions(items)
            .total(resultPage.getTotalElements())
            .page(page)
            .size(size)
            .build();
    }
}
