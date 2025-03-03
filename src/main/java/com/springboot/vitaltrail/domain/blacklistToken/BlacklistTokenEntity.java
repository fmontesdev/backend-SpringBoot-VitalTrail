package com.springboot.vitaltrail.domain.blacklistToken;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "blacklist_tokens")
// Identificador único. Hace que otras entidades puedan referenciar a esta y viceversa sin entrar en bucle en la serialización
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "idBlacklist")
public class BlacklistTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_blacklist")
    private Long idBlacklist;

    @Column(name = "refresh_token", nullable = false, length = 500)
    private String refreshToken;

    @Builder
    public BlacklistTokenEntity(Long idBlacklist, String refreshToken) {
        this.idBlacklist = idBlacklist;
        this.refreshToken = refreshToken;
    }
}
