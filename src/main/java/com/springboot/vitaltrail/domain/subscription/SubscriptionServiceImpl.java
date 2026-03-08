package com.springboot.vitaltrail.domain.subscription;

import com.springboot.vitaltrail.api.subscription.SubscriptionDto;
import com.springboot.vitaltrail.api.subscription.SubscriptionAssembler;

import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

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
}
