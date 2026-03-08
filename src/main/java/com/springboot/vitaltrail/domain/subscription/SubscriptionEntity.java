package com.springboot.vitaltrail.domain.subscription;

import com.springboot.vitaltrail.domain.user.UserEntity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscriptions")
// Identificador único. Hace que otras entidades puedan referenciar a esta y viceversa sin entrar en bucle en la serialización
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class SubscriptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "subscription_id", unique = true)
    private String subscriptionId;

    @Column(name = "subscription_type")
    private String subscriptionType;

    @Column(name = "billing_interval")
    private String billingInterval;  // month, year, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user")
    @JsonManagedReference // Marca este lado como "propietario"
    private UserEntity user;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "product_id")
    private String productId;
    
    @Column(name = "product_name")
    private String productName;
    
    @Column(name = "price_id")
    private String priceId;
    
    @Column(name = "current_period_start")
    private Long currentPeriodStart; // timestamp
    
    @Column(name = "current_period_end")
    private Long currentPeriodEnd; // timestamp
    
    @Column(name = "cancel_at_period_end")
    private Boolean cancelAtPeriodEnd;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column
    private String status;

    @Column(name = "last_event_type")
    private String lastEventType;
    
    // Campo que se crea automáticamente al insertar y nunca se actualiza
    @Column(name = "create_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // Campo que se actualiza automáticamente en cada modificación
    @Column(name = "update_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "payment_method_id")
    private String paymentMethodId;

    @Column(name = "payment_method_type")
    private String paymentMethodType;

    @Column(name = "card_brand")
    private String cardBrand;

    @Column(name = "card_last4")
    private String cardLast4;

    @Column(name = "card_exp_month")
    private Long cardExpMonth;

    @Column(name = "card_exp_year")
    private Long cardExpYear;

    @Builder
    public SubscriptionEntity(
        Long id,
        String subscriptionId,
        String subscriptionType,
        String billingInterval,
        UserEntity user,
        String customerId,
        String productId,
        String productName,
        String priceId,
        Long currentPeriodStart,
        Long currentPeriodEnd,
        Boolean cancelAtPeriodEnd,
        String cancellationReason,
        String status,
        String lastEventType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String paymentMethodId,
        String paymentMethodType,
        String cardBrand,
        String cardLast4,
        Long cardExpMonth,
        Long cardExpYear
    ){
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.subscriptionType = subscriptionType;
        this.billingInterval = billingInterval;
        this.user = user;
        this.customerId = customerId;
        this.productId = productId;
        this.productName = productName;
        this.priceId = priceId;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        this.cancellationReason = cancellationReason;
        this.status = status;
        this.lastEventType = lastEventType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.paymentMethodId = paymentMethodId;
        this.paymentMethodType = paymentMethodType;
        this.cardBrand = cardBrand;
        this.cardLast4 = cardLast4;
        this.cardExpMonth = cardExpMonth;
        this.cardExpYear = cardExpYear;
    }
}
