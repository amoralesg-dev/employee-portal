package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.LoginRequest;
import com.rassini.employeeportal.dto.request.RefreshTokenRequest;
import com.rassini.employeeportal.dto.request.ResetPasswordRequest;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.security.JwtService;
import com.rassini.employeeportal.service.AccessContextService;
import com.rassini.employeeportal.dto.response.UserAccessContextResponse;
import com.rassini.employeeportal.dto.request.LoginRequest;
import com.rassini.employeeportal.dto.request.RefreshTokenRequest;
import com.rassini.employeeportal.dto.request.ResetPasswordRequest;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.security.JwtService;
import com.rassini.employeeportal.service.AccessContextService;
import com.rassini.employeeportal.dto.response.UserAccessContextResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {
    @InjectMocks private AuthServiceImpl service;
    @Mock private UserRepository repository;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authManager;
    @Mock private AccessContextService accessService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private com.rassini.employeeportal.service.MfaService mfaService;

    @BeforeEach
    void setUp() { 
        MockitoAnnotations.openMocks(this); 
        ReflectionTestUtils.setField(service, "jwtExpirationMinutes", 60L);
    }

    @Test
    @DisplayName("login: usuario exitoso → LoginResponse no nulo")
    void testLogin_Success() {
        LoginRequest req = new LoginRequest(); req.setUsername("u"); req.setPassword("p");
        UserEntity u = new UserEntity(); u.setUsername("u"); u.setEnabled(true);
        when(repository.findByUsername("u")).thenReturn(Optional.of(u));
        when(jwtService.generateToken(any())).thenReturn("t");
        when(jwtService.generateRefreshToken(any())).thenReturn("rt");
        when(accessService.getAccessContext(any())).thenReturn(new UserAccessContextResponse());
        assertNotNull(service.login(req));
    }

    @Test
    @DisplayName("login: BadCredentialsException del AuthenticationManager → se propaga")
    void testLogin_BadCredentials() {
        LoginRequest req = new LoginRequest(); req.setUsername("u"); req.setPassword("wrong");
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authManager).authenticate(any());
        assertThrows(BadCredentialsException.class, () -> service.login(req));
    }

    @Test
    @DisplayName("login: usuario no encontrado → UsernameNotFoundException")
    void testLogin_NotFound() {
        LoginRequest req = new LoginRequest(); req.setUsername("u"); req.setPassword("p");
        when(repository.findByUsername("u")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.login(req));
    }

    @Test
    @DisplayName("login: usuario deshabilitado → DisabledException")
    void testLogin_InactiveUser() {
        LoginRequest req = new LoginRequest(); req.setUsername("u"); req.setPassword("p");
        UserEntity u = new UserEntity(); u.setUsername("u"); u.setEnabled(false);
        when(repository.findByUsername("u")).thenReturn(Optional.of(u));
        assertThrows(DisabledException.class, () -> service.login(req));
    }

    @Test
    void testGetMe_Success() {
        UserEntity u = new UserEntity(); u.setUsername("u"); u.setEnabled(true);
        when(repository.findByUsername("u")).thenReturn(Optional.of(u));
        when(accessService.getAccessContext(any())).thenReturn(new UserAccessContextResponse());
        assertNotNull(service.getMe("u"));
    }
    
    @Test
    void testGetMe_NotFound() {
        when(repository.findByUsername("u")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.getMe("u"));
    }
    
    @Test
    void testResetPassword_Success() {
        ResetPasswordRequest req = new ResetPasswordRequest(); req.setUsername("u"); req.setNewPassword("p");
        UserEntity u = new UserEntity(); u.setUsername("u");
        when(repository.findByUsername("u")).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("p")).thenReturn("x");
        service.resetPassword(req);
        verify(repository).save(u);
    }

    @Test
    void testResetPassword_NotFound() {
        ResetPasswordRequest req = new ResetPasswordRequest(); req.setUsername("u"); req.setNewPassword("p");
        when(repository.findByUsername("u")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.resetPassword(req));
    }

    @Test
    void testRefresh_Success() {
        RefreshTokenRequest req = new RefreshTokenRequest(); req.setRefreshToken("rt");
        UserEntity u = new UserEntity(); u.setUsername("u"); u.setEnabled(true);
        when(jwtService.extractUsername("rt")).thenReturn("u");
        when(repository.findByUsername("u")).thenReturn(Optional.of(u));
        when(jwtService.isTokenValid(eq("rt"), any())).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("t");
        when(jwtService.generateRefreshToken(any())).thenReturn("nrt");
        when(accessService.getAccessContext(any())).thenReturn(new UserAccessContextResponse());
        assertNotNull(service.refresh(req));
    }
    
    @Test
    void testRefresh_NotFound() {
        RefreshTokenRequest req = new RefreshTokenRequest(); req.setRefreshToken("rt");
        when(jwtService.extractUsername("rt")).thenReturn("u");
        when(repository.findByUsername("u")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.refresh(req));
    }
    
    @Test
    void testRefresh_InvalidToken() {
        RefreshTokenRequest req = new RefreshTokenRequest(); req.setRefreshToken("rt");
        UserEntity u = new UserEntity(); u.setUsername("u"); u.setEnabled(true);
        when(jwtService.extractUsername("rt")).thenReturn("u");
        when(repository.findByUsername("u")).thenReturn(Optional.of(u));
        when(jwtService.isTokenValid(eq("rt"), any())).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.refresh(req));
    }

    @Test
    void testLogin_MfaRequired_ForcedSetup() {
        LoginRequest req = new LoginRequest("u", "p");
        UserEntity u = new UserEntity();
        u.setId(1L);
        u.setUsername("u");
        u.setEmail("u@rassini.com");
        u.setEnabled(true);
        u.setMfaEnabled(true);
        u.setMfaSecret(null);

        when(repository.findByUsername("u")).thenReturn(Optional.of(u));
        when(jwtService.generateTempToken(eq(u), anyString(), eq(true))).thenReturn("temp_setup");
        when(mfaService.generateSetup(1L, "u@rassini.com")).thenReturn(
                com.rassini.employeeportal.dto.mfa.MfaSetupResponse.builder().qrCodeUri("data:image/png;base64,x").manualEntryKey("SECRET").build()
        );

        var res = service.login(req);
        assertTrue(res.isMfaRequired());
        assertTrue(res.getPendingResponse().isMfaSetupRequired());
    }

    @Test
    void testChangePassword_SuccessAndErrors() {
        com.rassini.employeeportal.dto.request.ChangePasswordRequest mismatch = new com.rassini.employeeportal.dto.request.ChangePasswordRequest();
        mismatch.setCurrentPassword("old");
        mismatch.setNewPassword("new1");
        mismatch.setConfirmPassword("new2");
        assertThrows(IllegalArgumentException.class, () -> service.changePassword("u", mismatch));

        com.rassini.employeeportal.dto.request.ChangePasswordRequest valid = new com.rassini.employeeportal.dto.request.ChangePasswordRequest();
        valid.setCurrentPassword("old");
        valid.setNewPassword("new1");
        valid.setConfirmPassword("new1");

        UserEntity u = new UserEntity();
        u.setUsername("u");
        u.setPasswordHash("hashed");
        when(repository.findByUsername("u")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("old", "hashed")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.changePassword("u", valid));

        when(passwordEncoder.matches("old", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("new1")).thenReturn("new_hashed");
        service.changePassword("u", valid);
        verify(repository).save(u);
    }

    @Test
    void testSetupActivateVerifyAndDisableMfa() {
        UserEntity u = new UserEntity();
        u.setId(1L);
        u.setUsername("u");
        u.setEmail("u@rassini.com");
        u.setEnabled(true);
        u.setMfaRequired(false);
        when(repository.findByUsername("u")).thenReturn(Optional.of(u));

        when(mfaService.generateSetup(1L, "u@rassini.com")).thenReturn(new com.rassini.employeeportal.dto.mfa.MfaSetupResponse());
        assertNotNull(service.setupMfa("u"));

        com.rassini.employeeportal.dto.mfa.MfaActivateRequest actReq = new com.rassini.employeeportal.dto.mfa.MfaActivateRequest();
        actReq.setCode("123456");
        service.activateMfa("u", actReq);
        verify(mfaService).activateMfa(1L, "123456");

        com.rassini.employeeportal.dto.mfa.MfaDisableRequest disReq = new com.rassini.employeeportal.dto.mfa.MfaDisableRequest();
        disReq.setPassword("p");
        disReq.setCode("123456");
        service.disableMfa("u", disReq);
        verify(mfaService).disableMfa(1L, "p", "123456");

        u.setMfaRequired(true);
        assertThrows(com.rassini.employeeportal.exception.BusinessException.class, () -> service.disableMfa("u", disReq));
    }
}
