package com.rassini.employeeportal.security;

import com.rassini.employeeportal.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CustomUserDetails implements UserDetails {

    private final UserEntity user;

    public CustomUserDetails(UserEntity user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                // Agregar rol con prefijo ROLE_
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
                
                // Agregar permisos directamente
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(permission -> {
                        authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                    });
                }
            });
        }
        
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
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
        return Boolean.TRUE.equals(user.getEnabled());
    }

    public UserEntity getUserEntity() {
        return user;
    }
}
