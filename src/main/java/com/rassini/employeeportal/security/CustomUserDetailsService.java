package com.rassini.employeeportal.security;

import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        
        // El framework llamará a getAuthorities() de CustomUserDetails, y esto accederá a las colecciones lazy.
        // Estando anotado con @Transactional(readOnly = true), la sesión estará activa.
        // Pero para asegurar que se carguen, inicializamos roles y permisos aquí.
        user.getRoles().size();
        user.getRoles().forEach(role -> {
            if (role.getPermissions() != null) {
                role.getPermissions().size();
            }
        });
        
        return new CustomUserDetails(user);
    }
}
