package com.springboot.vitaltrail.api.security.authorization;

import com.springboot.vitaltrail.api.security.AuthUtils;
import com.springboot.vitaltrail.domain.user.UserEntity;
import com.springboot.vitaltrail.domain.user.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthorizationConfig {
    private final AuthUtils authUtils;
    private final UserService userService;

    public boolean isAuthenticated() {
        return authUtils.isAuthenticated();
    }

    /**
     * Comprueba si el usuario autenticado es el propietario del customerId indicado.
     * Requiere que el usuario tenga un ClientEntity con ese customerId.
     *
     * @param customerId identificador del cliente en Stripe
     * @return true si el usuario es el propietario, false en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean isOwner(String customerId) {
        UUID idUser = authUtils.getCurrentUserId();
        UserEntity user = userService.getUserByIdUser(idUser);
        return user.getClient() != null && customerId.equals(user.getClient().getCustomerId());
    }
}
