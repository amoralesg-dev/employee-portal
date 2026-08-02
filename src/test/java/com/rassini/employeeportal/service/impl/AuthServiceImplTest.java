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
}
