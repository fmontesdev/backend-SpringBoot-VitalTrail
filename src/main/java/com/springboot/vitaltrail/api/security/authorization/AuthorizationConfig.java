package com.springboot.vitaltrail.api.security.authorization;

import com.springboot.vitaltrail.api.security.AuthUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationConfig {
    private final AuthUtils authUtils;

    public boolean isAuthenticated() {
        return authUtils.isAuthenticated();
    }
}
