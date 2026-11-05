package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.config.CacheConfig;
import com.rassini.employeeportal.dto.mfa.MfaSetupResponse;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.InvalidMfaSessionException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.MfaCryptoService;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.secret.SecretGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecretGenerator secretGenerator;

    @Mock
    private CodeVerifier codeVerifier;

    private CacheManager cacheManager;

    @Mock
    private MfaCryptoService mfaCryptoService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private MfaServiceImpl mfaService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager(
                CacheConfig.MFA_SETUP_CACHE,
                CacheConfig.MFA_CONSUMED_CACHE,
                CacheConfig.MFA_ATTEMPTS_CACHE
        );
        mfaService = new MfaServiceImpl(
                userRepository,
                secretGenerator,
                codeVerifier,
                cacheManager,
                mfaCryptoService,
                passwordEncoder
        );
        user = UserEntity.builder()
                .id(1L)
                .username("user1")
                .email("user1@rassini.com")
                .passwordHash("hashed")
                .mfaEnabled(false)
                .build();
    }

    @Test
    void generateSetup_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(secretGenerator.generate()).thenReturn("JBSWY3DPEHPK3PXP");

        MfaSetupResponse res = mfaService.generateSetup(1L, "user1@rassini.com");

        assertNotNull(res);
        assertEquals("JBSWY3DPEHPK3PXP", res.getManualEntryKey());
        assertTrue(res.getQrCodeUri().startsWith("data:image/png;base64,"));
        assertEquals("JBSWY3DPEHPK3PXP", cacheManager.getCache(CacheConfig.MFA_SETUP_CACHE).get(1L, String.class));
    }

    @Test
    void generateSetup_AlreadyConfigured_ThrowsBadRequest() {
        user.setMfaSecret("v1:encrypted");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertThrows(ResponseStatusException.class, () -> mfaService.generateSetup(1L, "user1@rassini.com"));
    }

    @Test
    void activateMfa_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        cacheManager.getCache(CacheConfig.MFA_SETUP_CACHE).put(1L, "JBSWY3DPEHPK3PXP");
        when(codeVerifier.isValidCode("JBSWY3DPEHPK3PXP", "123456")).thenReturn(true);
        when(mfaCryptoService.encrypt("JBSWY3DPEHPK3PXP")).thenReturn("v1:encrypted");

        mfaService.activateMfa(1L, "123456");

        assertTrue(user.getMfaEnabled());
        assertEquals("v1:encrypted", user.getMfaSecret());
        assertNotNull(user.getMfaEnabledAt());
        verify(userRepository).save(user);
    }

    @Test
    void activateMfa_ExpiredCache_ThrowsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertThrows(ResponseStatusException.class, () -> mfaService.activateMfa(1L, "123456"));
    }

    @Test
    void activateMfa_InvalidCode_ThrowsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        cacheManager.getCache(CacheConfig.MFA_SETUP_CACHE).put(1L, "JBSWY3DPEHPK3PXP");
        when(codeVerifier.isValidCode("JBSWY3DPEHPK3PXP", "000000")).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> mfaService.activateMfa(1L, "000000"));
    }

    @Test
    void verifyMfa_ForcedSetup_Success() {
        user.setMfaEnabled(true);
        user.setMfaSecret(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        cacheManager.getCache(CacheConfig.MFA_SETUP_CACHE).put(1L, "JBSWY3DPEHPK3PXP");
        when(codeVerifier.isValidCode("JBSWY3DPEHPK3PXP", "123456")).thenReturn(true);
        when(mfaCryptoService.encrypt("JBSWY3DPEHPK3PXP")).thenReturn("v1:encrypted");

        UserEntity verified = mfaService.verifyMfa(1L, "123456", "jti-setup-1", "MFA_SETUP");

        assertEquals("v1:encrypted", verified.getMfaSecret());
        verify(userRepository).save(user);
    }

    @Test
    void verifyMfa_InvalidTokenType_ThrowsInvalidMfaSession() {
        user.setMfaEnabled(true);
        user.setMfaSecret("v1:encrypted");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(InvalidMfaSessionException.class,
                () -> mfaService.verifyMfa(1L, "123456", "jti-1", "MFA_SETUP"));

        // Max attempts already reached in cache
        cacheManager.getCache(CacheConfig.MFA_ATTEMPTS_CACHE).put("jti-max", 3);
        assertThrows(ResponseStatusException.class,
                () -> mfaService.verifyMfa(1L, "123456", "jti-max", "MFA_VERIFY"));

        // Forced setup with expired setupCache
        user.setMfaSecret(null);
        assertThrows(ResponseStatusException.class,
                () -> mfaService.verifyMfa(1L, "123456", "jti-exp-setup", "MFA_SETUP"));
    }

    @Test
    void disableMfa_SuccessAndValidationErrors() {
        user.setMfaEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertThrows(ResponseStatusException.class, () -> mfaService.disableMfa(1L, "pass", "123456"));

        user.setMfaEnabled(true);
        user.setMfaSecret("v1:encrypted");
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> mfaService.disableMfa(1L, "wrong", "123456"));

        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(mfaCryptoService.decrypt("v1:encrypted")).thenReturn("JBSWY3DPEHPK3PXP");
        when(codeVerifier.isValidCode("JBSWY3DPEHPK3PXP", "000000")).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> mfaService.disableMfa(1L, "pass", "000000"));

        when(codeVerifier.isValidCode("JBSWY3DPEHPK3PXP", "123456")).thenReturn(true);
        mfaService.disableMfa(1L, "pass", "123456");

        assertFalse(user.getMfaEnabled());
        assertNull(user.getMfaSecret());
        verify(userRepository).save(user);
    }
}
