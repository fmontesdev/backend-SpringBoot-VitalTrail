package com.springboot.vitaltrail.domain.user.admin;

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
@Table(name = "admins")
public class AdminEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_admin")
    private Long idAdmin;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", referencedColumnName = "id_user", nullable = false)
    private UserEntity user;
    
    @Builder
    public AdminEntity(Long idAdmin, UserEntity user) {
        this.idAdmin = idAdmin;
        this.user = user;
    }
}
