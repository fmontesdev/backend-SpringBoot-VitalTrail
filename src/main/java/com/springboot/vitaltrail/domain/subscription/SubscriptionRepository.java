package com.springboot.vitaltrail.domain.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long>, JpaSpecificationExecutor<SubscriptionEntity> {

    Optional<SubscriptionEntity> findBySubscriptionId(String subscriptionId);
    
    List<SubscriptionEntity> findByUserIdUser(UUID idUser);
    
    Optional<SubscriptionEntity> findByUserIdUserAndStatus(UUID idUser, String status);

    Optional<SubscriptionEntity> findByCustomerIdAndStatus(String customerId, String status);
}
