package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.config.CacheConfig;
import com.rassini.employeeportal.dto.mfa.MfaSetupResponse;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.MfaCryptoService;
import com.rassini.employeeportal.service.MfaService;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {

    private final UserRepository userRepository;
    private final SecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;
    private final CacheManager cacheManager;
    private final MfaCryptoService mfaCryptoService;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_ATTEMPTS = 3;

    @Override
    public MfaSetupResponse generateSetup(Long userId, String email) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getMfaSecret() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta ya tiene MFA habilitado.");
        }

        String secret = secretGenerator.generate();

        Cache setupCache = cacheManager.getCache(CacheConfig.MFA_SETUP_CACHE);
        if (setupCache != null) {
            setupCache.put(userId, secret);
        }

        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("EmployeePortal")
                .algorithm(dev.samstevens.totp.code.HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        try {
            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            java.util.Map<com.google.zxing.EncodeHintType, Object> hints = new java.util.HashMap<>();
            hints.put(com.google.zxing.EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H);
            hints.put(com.google.zxing.EncodeHintType.MARGIN, 1);

            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(
                    data.getUri(), 
                    com.google.zxing.BarcodeFormat.QR_CODE, 
                    350, 350, 
                    hints
            );
            
            java.io.ByteArrayOutputStream pngOutputStream = new java.io.ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] imageData = pngOutputStream.toByteArray();
            
            String mimeType = "image/png";
            String dataUri = dev.samstevens.totp.util.Utils.getDataUriForImage(imageData, mimeType);
            
            return MfaSetupResponse.builder()
                    .qrCodeUri(dataUri)
                    .manualEntryKey(secret)
                    .build();
        } catch (Exception e) {
            log.error("Error generando QR para usuario {}", userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno generando codigo QR.");
        }
    }

    @Override
    @Transactional
    public void activateMfa(Long userId, String code) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getMfaSecret() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta ya tiene MFA habilitado.");
        }

        Cache setupCache = cacheManager.getCache(CacheConfig.MFA_SETUP_CACHE);
        String secret = setupCache != null ? setupCache.get(userId, String.class) : null;

        if (secret == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tiempo de configuracion ha expirado. Solicite un nuevo codigo QR.");
        }

        if (!codeVerifier.isValidCode(secret, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El codigo de verificacion es incorrecto.");
        }

        String encryptedSecret = mfaCryptoService.encrypt(secret);
        user.setMfaEnabled(true);
        user.setMfaSecret(encryptedSecret);
        user.setMfaEnabledAt(LocalDateTime.now());
        userRepository.save(user);

        if (setupCache != null) {
            setupCache.evict(userId);
        }
        
        log.info("MFA habilitado correctamente para el usuario {}", userId);
    }

    @Override
    public UserEntity verifyMfa(Long userId, String code, String jti, String tokenType) {
        Cache consumedCache = cacheManager.getCache(CacheConfig.MFA_CONSUMED_CACHE);
        if (consumedCache != null && consumedCache.get(jti) != null) {
            throw new com.rassini.employeeportal.exception.InvalidMfaSessionException(
                "MFA_SESSION_CONSUMED",
                "Token temporal ya utilizado."
            );
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA no esta configurado para esta cuenta.");
        }

        boolean isForcedSetup = (user.getMfaSecret() == null);
        String expectedTokenType = isForcedSetup ? "MFA_SETUP" : "MFA_VERIFY";
        if (!expectedTokenType.equals(tokenType)) {
            throw new com.rassini.employeeportal.exception.InvalidMfaSessionException(
                "INVALID_MFA_SESSION",
                "La sesión de verificación no es válida. Inicia sesión nuevamente."
            );
        }

        Cache attemptsCache = cacheManager.getCache(CacheConfig.MFA_ATTEMPTS_CACHE);
        Integer attempts = 0;
        if (attemptsCache != null) {
            attempts = attemptsCache.get(jti, Integer.class);
            if (attempts == null) {
                attempts = 0;
            }
            if (attempts >= MAX_ATTEMPTS) {
                log.warn("Límite de intentos de MFA excedido para JTI {}", jti);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite de intentos excedido. Vuelva a iniciar sesion.");
            }
        }

        String decryptedSecret;
        
        if (isForcedSetup) {
            Cache setupCache = cacheManager.getCache(CacheConfig.MFA_SETUP_CACHE);
            String secret = setupCache != null ? setupCache.get(userId, String.class) : null;
            if (secret == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tiempo de configuracion ha expirado. Solicite un nuevo codigo QR.");
            }
            decryptedSecret = secret;
        } else {
            decryptedSecret = mfaCryptoService.decrypt(user.getMfaSecret());
        }

        if (!codeVerifier.isValidCode(decryptedSecret, code)) {
            if (attemptsCache != null) {
                attemptsCache.put(jti, attempts + 1);
                if (attempts + 1 >= MAX_ATTEMPTS) {
                    if (consumedCache != null) {
                        consumedCache.put(jti, true);
                    }
                }
            }
            log.warn("Intento fallido de MFA para usuario {}", userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El codigo de verificacion es incorrecto.");
        }
        
        // Consumir el jti atómicamente antes de aplicar efectos secundarios para evitar carreras concurrentes
        if (consumedCache != null) {
            org.springframework.cache.Cache.ValueWrapper existing = consumedCache.putIfAbsent(jti, true);
            if (existing != null) {
                throw new com.rassini.employeeportal.exception.InvalidMfaSessionException(
                    "MFA_SESSION_CONSUMED",
                    "Token temporal ya utilizado."
                );
            }
        }
        
        if (isForcedSetup) {
            String encryptedSecret = mfaCryptoService.encrypt(decryptedSecret);
            user.setMfaSecret(encryptedSecret);
            user.setMfaEnabledAt(LocalDateTime.now());
            userRepository.save(user);
            
            Cache setupCache = cacheManager.getCache(CacheConfig.MFA_SETUP_CACHE);
            if (setupCache != null) {
                setupCache.evict(userId);
            }
            log.info("MFA enrolado y verificado con exito (Setup Forzado) para usuario {}", userId);
        }

        if (attemptsCache != null) {
            attemptsCache.evict(jti);
        }
        log.info("MFA verificado con exito para usuario {}", userId);

        return user;
    }

    @Override
    @Transactional
    public void disableMfa(Long userId, String password, String code) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA no esta configurado para esta cuenta.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña o el codigo de verificacion son incorrectos.");
        }

        String decryptedSecret = mfaCryptoService.decrypt(user.getMfaSecret());
        if (!codeVerifier.isValidCode(decryptedSecret, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña o el codigo de verificacion son incorrectos.");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaEnabledAt(null);
        userRepository.save(user);

        log.info("MFA deshabilitado para usuario {}", userId);
    }
}
