package com.springboot.vitaltrail.api.security;

import java.util.Collection;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthUtils {
    public UUID getCurrentUserId() {
        return getUserDetails().getId();
    }

    public String getCurrentUserUsername() {
        return getAuthentication().getName();
    }

    public String getCurrentUserEmail() {
        return getUserDetails().getEmail();
    }

    public Collection<? extends GrantedAuthority> getCurrentUserRole() {
        return getAuthentication().getAuthorities();
    }

    public boolean isAuthenticated() {
        return getAuthentication() != null;
    }

    public  boolean isAnonymousRole() {
        return getCurrentUserRole().stream()
            .anyMatch(role -> role.getAuthority().equals("ROLE_ANONYMOUS"));
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private UserDetailsImpl getUserDetails() {
        return (UserDetailsImpl) getAuthentication().getPrincipal();
    }
}
