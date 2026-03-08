package com.springboot.vitaltrail.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
    Optional<UserEntity> findByIdUser(UUID idUser);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);
}
