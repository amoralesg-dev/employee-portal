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
    private final com.rassini.employeeportal.service.MfaService mfaService;

    @Value("${app.security.jwt.expiration-minutes}")
    private long jwtExpirationMinutes;

    @Override
    @Transactional(readOnly = true)
    public com.rassini.employeeportal.dto.auth.LoginResult login(LoginRequest request) {
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

        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            String jti = java.util.UUID.randomUUID().toString();
            boolean setupRequired = user.getMfaSecret() == null;
            String tempToken = jwtService.generateTempToken(user, jti, setupRequired);
            
            int expiresInSeconds = setupRequired ? (15 * 60) : (5 * 60);

            com.rassini.employeeportal.dto.mfa.MfaPendingResponse.MfaPendingResponseBuilder pendingBuilder = com.rassini.employeeportal.dto.mfa.MfaPendingResponse.builder()
                    .mfaRequired(true)
                    .mfaSetupRequired(setupRequired)
                    .tempToken(tempToken)
                    .expiresIn(expiresInSeconds)
                    .message(setupRequired ? "Se requiere configurar factor multiple" : "Se requiere validacion de factor multiple");

            if (setupRequired) {
                com.rassini.employeeportal.dto.mfa.MfaSetupResponse setup = mfaService.generateSetup(user.getId(), user.getEmail());
                pendingBuilder.qrCodeUri(setup.getQrCodeUri())
                              .manualEntryKey(setup.getManualEntryKey());
            }

            return com.rassini.employeeportal.dto.auth.LoginResult.builder()
                    .mfaRequired(true)
                    .pendingResponse(pendingBuilder.build())
                    .build();
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        log.debug("Generando token para el usuario: {}", user.getUsername());
        log.debug("Token generado en login(): {}", token);
        log.debug("Expiracion configurada en login() (minutos): {}", jwtExpirationMinutes);

        return com.rassini.employeeportal.dto.auth.LoginResult.builder()
                .mfaRequired(false)
                .successResponse(buildLoginResponse(user, token, refreshToken))
                .build();
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
                .forcePasswordChange(user.getForcePasswordChange())
                .mfaEnabled(Boolean.TRUE.equals(user.getMfaEnabled()))
                .mfaRequired(Boolean.TRUE.equals(user.getMfaRequired()))
                .build();

        UserAccessContextResponse accessContext = accessContextService.getAccessContext(user.getId());

        return com.rassini.employeeportal.dto.response.MeResponse.builder()
                .user(authUser)
                .roles(accessContext.getRoles())
                .permissions(accessContext.getPermissions())
                .menus(accessContext.getMenus())
                .businessUnits(accessContext.getBusinessUnits())
                .hasAllBusinessUnits(accessContext.getHasAllBusinessUnits())
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(com.rassini.employeeportal.dto.request.ResetPasswordRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + request.getUsername()));
        
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setForcePasswordChange(true);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(String username, com.rassini.employeeportal.dto.request.ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("La nueva contraseña y la confirmación no coinciden");
        }

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setForcePasswordChange(false);
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
                .forcePasswordChange(user.getForcePasswordChange())
                .mfaEnabled(Boolean.TRUE.equals(user.getMfaEnabled()))
                .mfaRequired(Boolean.TRUE.equals(user.getMfaRequired()))
                .build();

        UserAccessContextResponse accessContext = accessContextService.getAccessContext(user.getId());

        LoginResponse.LoginResponseBuilder builder = LoginResponse.builder()
                .user(authUser)
                .roles(accessContext.getRoles())
                .permissions(accessContext.getPermissions())
                .menus(accessContext.getMenus())
                .businessUnits(accessContext.getBusinessUnits())
                .hasAllBusinessUnits(accessContext.getHasAllBusinessUnits());
        
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

    @Override
    @Transactional(readOnly = true)
    public com.rassini.employeeportal.dto.mfa.MfaSetupResponse setupMfa(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        return mfaService.generateSetup(user.getId(), user.getEmail());
    }

    @Override
    @Transactional
    public void activateMfa(String username, com.rassini.employeeportal.dto.mfa.MfaActivateRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        mfaService.activateMfa(user.getId(), request.getCode());
    }

    @Override
    @Transactional
    public LoginResponse verifyMfa(com.rassini.employeeportal.dto.mfa.MfaVerifyRequest request) {
        String tempToken = request.getTempToken();
        Long userId = jwtService.extractClaim(tempToken, claims -> claims.get("user_id", Long.class));
        String jti = jwtService.extractClaim(tempToken, claims -> claims.get("jti", String.class));
        String tokenType = jwtService.extractClaim(tempToken, claims -> claims.get("token_type", String.class));

        if (!"MFA_VERIFY".equals(tokenType) && !"MFA_SETUP".equals(tokenType)) {
            throw new com.rassini.employeeportal.exception.InvalidMfaSessionException(
                "INVALID_MFA_SESSION", 
                "La sesión de verificación no es válida. Inicia sesión nuevamente."
            );
        }

        UserEntity user = mfaService.verifyMfa(userId, request.getCode(), jti, tokenType);

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new org.springframework.security.authentication.DisabledException("La cuenta de usuario está deshabilitada");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return buildLoginResponse(user, token, refreshToken);
    }

    @Override
    @Transactional
    public void disableMfa(String username, com.rassini.employeeportal.dto.mfa.MfaDisableRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        
        if (Boolean.TRUE.equals(user.getMfaRequired())) {
            throw new com.rassini.employeeportal.exception.BusinessException("MFA es obligatorio para esta cuenta y no puede ser deshabilitado por el usuario.");
        }
        
        mfaService.disableMfa(user.getId(), request.getPassword(), request.getCode());
    }
}
