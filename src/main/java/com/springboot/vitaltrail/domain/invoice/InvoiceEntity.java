package com.springboot.vitaltrail.domain.invoice;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.springboot.vitaltrail.domain.subscription.SubscriptionEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
// import lombok.Builder;
import jakarta.persistence.*;
// import org.hibernate.annotations.CreationTimestamp;
// import org.hibernate.annotations.UpdateTimestamp;
// import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoices")
// Identificador único. Hace que otras entidades puedan referenciar a esta y viceversa sin entrar en bucle en la serialización
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class InvoiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    @JsonManagedReference // Marca este lado como "propietario"
    private SubscriptionEntity subscription;
    
    @Column(name = "stripe_invoice_id")
    private String stripeInvoiceId;
    
    @Column(name = "amount_paid")
    private Long amountPaid;
    
    @Column
    private String currency;
    
    @Column(name = "invoice_date")
    private Long invoiceDate;
    
    @Column
    private String status;  // paid, open, void, uncollectible
    
    // Otros campos relevantes para facturas
}
