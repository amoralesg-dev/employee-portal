package com.rassini.employeeportal.config;

import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Runner de inicialización (SOLO DEV) para crear o actualizar el usuario admin por defecto.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevAdminUserSetupRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        String username = "admin";
        String rawPassword = "Admin123!";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        UserEntity admin = userRepository.findByUsername(username).orElse(null);

        if (admin == null) {
            admin = UserEntity.builder()
                    .username(username)
                    .email("admin@rassini.com")
                    .enabled(true)
                    .passwordHash(encodedPassword)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            log.info("Creando nuevo usuario admin...");
        } else {
            admin.setPasswordHash(encodedPassword);
            admin.setUpdatedAt(LocalDateTime.now());
            log.info("Actualizando usuario admin existente...");
        }

        userRepository.save(admin);
        log.info("Setup de usuario admin completado. Password Raw: '{}' | Hash: '{}'", rawPassword, encodedPassword);
    }
}
