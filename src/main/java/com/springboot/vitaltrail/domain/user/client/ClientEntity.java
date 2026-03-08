package com.springboot.vitaltrail.domain.user.client;

import com.springboot.vitaltrail.domain.user.UserEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clients")
public class ClientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_client")
    private Long idClient;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", referencedColumnName = "id_user", nullable = false)
    private UserEntity user;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "customer_id")
    private String customerId = null;

    @Column(name = "payment_method_id")
    private String paymentMethodId = null;
    
    @Builder
    public ClientEntity(
        Long idClient,
        UserEntity user,
        String phone,
        String customerId,
        String paymentMethodId
    ) {
        this.idClient = idClient;
        this.user = user;
        this.phone = phone;
        this.customerId = customerId;
        this.paymentMethodId = paymentMethodId;
    }
}
