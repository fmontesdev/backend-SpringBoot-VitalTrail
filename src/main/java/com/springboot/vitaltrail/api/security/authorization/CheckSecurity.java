package com.springboot.vitaltrail.api.security.authorization;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public @interface CheckSecurity {
    public @interface Public {
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        @PreAuthorize("permitAll()")
        public @interface canRead {}
    }

    public @interface Protected {
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        @PreAuthorize("@authorizationConfig.isAuthenticated()")
        public @interface canManage {}

        /**
         * Requiere que el usuario sea ADMIN o el propietario del customerId indicado.
         * Usar cuando el customerId llega como @PathVariable String customerId.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        @PreAuthorize("hasRole('ROLE_ADMIN') or @authorizationConfig.isOwner(#customerId)")
        public @interface isAdminOrOwner {}

        /**
         * Requiere que el usuario sea ADMIN o el propietario del customerId indicado.
         * Usar cuando el customerId llega dentro de un @RequestBody con campo customerId
         * (p. ej. SubscriptionActionDto request → #request.customerId).
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        @PreAuthorize("hasRole('ROLE_ADMIN') or @authorizationConfig.isOwner(#request.customerId)")
        public @interface isAdminOrOwnerByAction {}
    }

}
