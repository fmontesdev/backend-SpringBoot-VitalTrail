package com.springboot.vitaltrail.domain.refreshToken;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "refresh_tokens",
    uniqueConstraints = @UniqueConstraint(columnNames = "id_user")
)
// Identificador único. Hace que otras entidades puedan referenciar a esta y viceversa sin entrar en bucle en la serialización
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "idRefresh")
public class RefreshTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_refresh")
    private Long idRefresh;

    @JdbcTypeCode(SqlTypes.VARCHAR) // Usa VARCHAR para almacenar el UUID como texto
    @Column(name = "id_user", nullable = false)
    private UUID idUser;

    @Column(name = "refresh_token", nullable = false, length = 500)
    private String refreshToken;

    @Builder
    public RefreshTokenEntity(Long idRefresh, UUID idUser, String refreshToken) {
        this.idRefresh = idRefresh;
        this.idUser = idUser;
        this.refreshToken = refreshToken;
    }
}
