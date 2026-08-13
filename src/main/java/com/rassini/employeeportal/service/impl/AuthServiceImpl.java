package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.LoginRequest;
import com.rassini.employeeportal.dto.response.AuthenticatedUserResponse;
import com.rassini.employeeportal.dto.response.LoginResponse;
import com.rassini.employeeportal.dto.response.UserAccessContextResponse;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.security.CustomUserDetails;
import com.rassini.employeeportal.security.JwtService;
import com.rassini.employeeportal.service.AccessContextService;
import com.rassini.employeeportal.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AccessContextService accessContextService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.jwt.expiration-minutes}")
    private long jwtExpirationMinutes;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + request.getUsername()));

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new DisabledException("Usuario deshabilitado: " + user.getUsername());
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        log.debug("Generando token para el usuario: {}", user.getUsername());
        log.debug("Token generado en login(): {}", token);
        log.debug("Expiración configurada en login() (minutos): {}", jwtExpirationMinutes);

        return buildLoginResponse(user, token, refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public com.rassini.employeeportal.dto.response.MeResponse getMe(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        
        AuthenticatedUserResponse authUser = AuthenticatedUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .enabled(user.getEnabled())
                .build();

        UserAccessContextResponse accessContext = accessContextService.getAccessContext(user.getId());

        return com.rassini.employeeportal.dto.response.MeResponse.builder()
                .user(authUser)
                .roles(accessContext.getRoles())
                .permissions(accessContext.getPermissions())
                .menus(accessContext.getMenus())
                .businessUnits(accessContext.getBusinessUnits())
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(com.rassini.employeeportal.dto.request.ResetPasswordRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + request.getUsername()));
        
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse refresh(com.rassini.employeeportal.dto.request.RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String username = jwtService.extractUsername(refreshToken);
        
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
                
        CustomUserDetails userDetails = new CustomUserDetails(user);
        
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new RuntimeException("Refresh token inválido o expirado");
        }
        
        String newToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);
        
        return buildLoginResponse(user, newToken, newRefreshToken);
    }

    private LoginResponse buildLoginResponse(UserEntity user, String token, String refreshToken) {
        AuthenticatedUserResponse authUser = AuthenticatedUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .enabled(user.getEnabled())
                .build();

        UserAccessContextResponse accessContext = accessContextService.getAccessContext(user.getId());

        LoginResponse.LoginResponseBuilder builder = LoginResponse.builder()
                .user(authUser)
                .roles(accessContext.getRoles())
                .permissions(accessContext.getPermissions())
                .menus(accessContext.getMenus())
                .businessUnits(accessContext.getBusinessUnits());
        
        log.debug("Token recibido en buildLoginResponse: {}", token);
        
        if (token != null) {
            builder = builder.accessToken(token)
                             .refreshToken(refreshToken)
                             .expiresIn(jwtExpirationMinutes * 60 * 1000); // en ms
            log.debug("Token y refresh token asignados al builder correctamente.");
        } else {
            log.debug("El token es NULL en buildLoginResponse, no se asigna al builder (esto es normal para /me).");
        }
        
        LoginResponse response = builder.build();
        log.debug("LoginResponse final -> accessToken: {}, expiresIn: {}", response.getAccessToken(), response.getExpiresIn());
        
        return response;
    }
}
