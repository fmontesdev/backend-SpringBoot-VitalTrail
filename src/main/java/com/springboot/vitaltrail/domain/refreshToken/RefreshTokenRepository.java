package com.springboot.vitaltrail.domain.refreshToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long>, JpaSpecificationExecutor<RefreshTokenEntity> {

    Optional<RefreshTokenEntity> findByIdUser(UUID idUser);
}
