package com.rassini.employeeportal.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(jwtService, "secretKey", "CHANGE_ME_DEV_SECRET_CHANGE_ME_DEV_SECRET_123456789_ANOTHER_LONG_PART");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMinutes", 60L);
        userDetails = new User("admin", "pass", Collections.emptyList());
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtService.generateToken(userDetails);
        assertNotNull(token);
        
        String token2 = jwtService.generateToken(new HashMap<>(), userDetails);
        assertNotNull(token2);
        
        String username = jwtService.extractUsername(token);
        assertEquals("admin", username);
        
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void testGenerateRefreshToken() {
        String token = jwtService.generateRefreshToken(userDetails);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void testExtractSpecificClaim() {
        String token = jwtService.generateToken(userDetails);
        String subject = jwtService.extractClaim(token, Claims::getSubject);
        assertEquals("admin", subject);
    }

    @Test
    void testTokenValidationFailureDifferentUser() {
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUser = new User("otherUser", "pass", Collections.emptyList());
        assertFalse(jwtService.isTokenValid(token, otherUser));
    }
}
