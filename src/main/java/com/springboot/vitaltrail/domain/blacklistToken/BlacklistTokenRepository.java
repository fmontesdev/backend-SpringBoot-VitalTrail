package com.springboot.vitaltrail.domain.blacklistToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlacklistTokenRepository extends JpaRepository<BlacklistTokenEntity, Long>, JpaSpecificationExecutor<BlacklistTokenEntity> {

    Optional<BlacklistTokenEntity> findByRefreshToken(String refreshToken);
}
