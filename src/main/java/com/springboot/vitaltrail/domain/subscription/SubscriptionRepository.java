package com.springboot.vitaltrail.domain.subscription;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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

    // ── Admin stats ──────────────────────────────────────────────────────────

    long countByStatus(String status);

    long countByStatusAndBillingInterval(String status, String billingInterval);

    @Query(value = "SELECT COUNT(*) FROM subscriptions " +
           "WHERE EXTRACT(MONTH FROM create_at) = EXTRACT(MONTH FROM NOW()) " +
           "AND EXTRACT(YEAR FROM create_at) = EXTRACT(YEAR FROM NOW())",
           nativeQuery = true)
    long countNewThisMonth();

    @Query(value = "SELECT COUNT(*) FROM subscriptions " +
           "WHERE status = 'canceled' " +
           "AND EXTRACT(MONTH FROM update_at) = EXTRACT(MONTH FROM NOW()) " +
           "AND EXTRACT(YEAR FROM update_at) = EXTRACT(YEAR FROM NOW())",
           nativeQuery = true)
    long countCanceledThisMonth();

    // ── Admin paged list ─────────────────────────────────────────────────────

    Page<SubscriptionEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<SubscriptionEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
