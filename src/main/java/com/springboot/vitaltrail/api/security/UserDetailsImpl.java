package com.springboot.vitaltrail.api.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

public class UserDetailsImpl implements UserDetails {
    private UUID id;
    private String username;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities; // Roles o permisos del usuario

    // Constructor
    public UserDetailsImpl(
        UUID id,
        String username,
        String email,
        String password, 
        Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    // Métodos adicionales para acceder a los datos personalizados
    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    // Métodos sobrescritos de UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
