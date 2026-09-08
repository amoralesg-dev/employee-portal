package com.rassini.employeeportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rassini.employeeportal.dto.request.LoginRequest;
import com.rassini.employeeportal.dto.mfa.MfaVerifyRequest;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.MfaCryptoService;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-e2e")
public class E2ERealDbTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MfaCryptoService mfaCryptoService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String RAW_SECRET = "JBSWY3DPEHPK3PXP";
    private final String RAW_PASSWORD = "Password123!";
    
    private String userNoMfaName;
    private String userWithMfaName;

    @BeforeEach
    void setup() {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        userNoMfaName = "user_no_mfa_" + uuid;
        userWithMfaName = "user_with_mfa_" + uuid;

        UserEntity userNoMfa = UserEntity.builder()
                .username(userNoMfaName)
                .email("nomfa_" + uuid + "@rassini.com")
                .passwordHash(passwordEncoder.encode(RAW_PASSWORD))
                .enabled(true)
                .mfaEnabled(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        userRepository.save(userNoMfa);

        UserEntity userWithMfa = UserEntity.builder()
                .username(userWithMfaName)
                .email("withmfa_" + uuid + "@rassini.com")
                .passwordHash(passwordEncoder.encode(RAW_PASSWORD))
                .enabled(true)
                .mfaEnabled(true)
                .mfaSecret(mfaCryptoService.encrypt(RAW_SECRET))
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        userRepository.save(userWithMfa);
    }

    @AfterEach
    void cleanup() {
        if (userNoMfaName != null) {
            userRepository.findByUsername(userNoMfaName).ifPresent(userRepository::delete);
        }
        if (userWithMfaName != null) {
            userRepository.findByUsername(userWithMfaName).ifPresent(userRepository::delete);
        }
    }

    private String generateValidTotp() throws Exception {
        CodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
        long currentBucket = Math.floorDiv(System.currentTimeMillis() / 1000L, 30L);
        return generator.generate(RAW_SECRET, currentBucket);
    }

    @Test
    void executeAllScenarios() throws Exception {
        System.out.println("=== 0. Verificacion de Entorno y Base de Datos ===");
        String dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        String hostname = jdbcTemplate.queryForObject("SELECT @@hostname", String.class);
        String port = jdbcTemplate.queryForObject("SELECT @@port", String.class);
        System.out.println("DATABASE: " + dbName);
        System.out.println("HOSTNAME: " + hostname);
        System.out.println("PORT: " + port);
        System.out.println("Nombres de usuario generados: " + userNoMfaName + ", " + userWithMfaName);

        assertThat(dbName).isEqualTo("iam_test"); // Validacion estricta

        System.out.println("\n=== 1. Login sin MFA ===");
        LoginRequest req1 = new LoginRequest(userNoMfaName, RAW_PASSWORD);
        ResponseEntity<Map> res1 = restTemplate.postForEntity("/api/v1/auth/login", req1, Map.class);
        assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res1.getBody()).containsKey("accessToken");
        assertThat(res1.getBody()).containsKey("refreshToken");
        System.out.println("-> OK: HTTP 200, LoginResponse completo");

        System.out.println("\n=== 2. Login con MFA habilitado ===");
        LoginRequest req2 = new LoginRequest(userWithMfaName, RAW_PASSWORD);
        ResponseEntity<Map> res2 = restTemplate.postForEntity("/api/v1/auth/login", req2, Map.class);
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(res2.getBody()).containsKey("tempToken");
        assertThat(res2.getBody().get("expiresIn").toString()).isEqualTo("300");
        assertThat(res2.getBody()).doesNotContainKey("accessToken"); // Sin tokens completos
        String tempToken = (String) res2.getBody().get("tempToken");
        System.out.println("-> OK: HTTP 202, tempToken obtenido, expiresIn=300");

        System.out.println("\n=== 4. TempToken rechazado en Authorization (GET /auth/me) ===");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tempToken);
        ResponseEntity<String> res4 = restTemplate.exchange("/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        assertThat(res4.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        System.out.println("-> OK: Rechazado con 403");

        System.out.println("\n=== 5. TempToken rechazado en /mfa/verify cuando viaja por Authorization ===");
        MfaVerifyRequest verifyReq = new MfaVerifyRequest();
        verifyReq.setTempToken("invalid_in_body");
        verifyReq.setCode("123456");
        ResponseEntity<String> res5 = restTemplate.exchange("/api/v1/auth/mfa/verify", HttpMethod.POST, new HttpEntity<>(verifyReq, headers), String.class);
        assertThat(res5.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        System.out.println("-> OK: Rechazado con 403 por el JwtAuthenticationFilter");

        System.out.println("\n=== 3. Verificacion MFA exitosa ===");
        MfaVerifyRequest verifyReqSuccess = new MfaVerifyRequest();
        verifyReqSuccess.setTempToken(tempToken);
        verifyReqSuccess.setCode(generateValidTotp());
        ResponseEntity<Map> res3 = restTemplate.postForEntity("/api/v1/auth/mfa/verify", verifyReqSuccess, Map.class);
        assertThat(res3.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res3.getBody()).containsKey("accessToken");
        String accessToken = (String) res3.getBody().get("accessToken");
        System.out.println("-> OK: HTTP 200, LoginResponse completo");

        System.out.println("\n=== 11. Evidencia de que /auth/me funciona con AccessToken normal ===");
        HttpHeaders headersAuth = new HttpHeaders();
        headersAuth.set("Authorization", "Bearer " + accessToken);
        ResponseEntity<Map> res11 = restTemplate.exchange("/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(null, headersAuth), Map.class);
        assertThat(res11.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res11.getBody()).containsKey("user");
        System.out.println("-> OK: HTTP 200");

        System.out.println("\n=== 6. JTI consumido (Replay attack) ===");
        ResponseEntity<Map> res6 = restTemplate.postForEntity("/api/v1/auth/mfa/verify", verifyReqSuccess, Map.class);
        assertThat(res6.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res6.getBody().get("message").toString()).contains("ya utilizado");
        System.out.println("-> OK: HTTP 400 - Token ya utilizado");

        System.out.println("\n=== 7. Limite de intentos ===");
        ResponseEntity<Map> res7Login = restTemplate.postForEntity("/api/v1/auth/login", req2, Map.class);
        String tempToken2 = (String) res7Login.getBody().get("tempToken");
        MfaVerifyRequest failReq = new MfaVerifyRequest();
        failReq.setTempToken(tempToken2);
        failReq.setCode("000000"); // Malo
        
        restTemplate.postForEntity("/api/v1/auth/mfa/verify", failReq, Map.class);
        restTemplate.postForEntity("/api/v1/auth/mfa/verify", failReq, Map.class);
        restTemplate.postForEntity("/api/v1/auth/mfa/verify", failReq, Map.class);
        
        MfaVerifyRequest goodReqButBlocked = new MfaVerifyRequest();
        goodReqButBlocked.setTempToken(tempToken2);
        goodReqButBlocked.setCode(generateValidTotp());
        ResponseEntity<Map> res7Fail = restTemplate.postForEntity("/api/v1/auth/mfa/verify", goodReqButBlocked, Map.class);
        System.out.println("-> res7Fail body: " + res7Fail.getBody());
        assertThat(res7Fail.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        System.out.println("-> OK: HTTP " + res7Fail.getStatusCode() + " - " + res7Fail.getBody().get("message"));

        System.out.println("\n=== 8. Usuario deshabilitado despues de emitir tempToken ===");
        ResponseEntity<Map> res8Login = restTemplate.postForEntity("/api/v1/auth/login", req2, Map.class);
        String tempToken3 = (String) res8Login.getBody().get("tempToken");
        UserEntity u = userRepository.findByUsername(userWithMfaName).get();
        u.setEnabled(false);
        userRepository.save(u);
        MfaVerifyRequest req8 = new MfaVerifyRequest();
        req8.setTempToken(tempToken3);
        req8.setCode(generateValidTotp());
        ResponseEntity<Map> res8 = restTemplate.postForEntity("/api/v1/auth/mfa/verify", req8, Map.class);
        assertThat(res8.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        System.out.println("-> OK: HTTP " + res8.getStatusCode());

        System.out.println("\n=== 9. MFA deshabilitado despues de emitir tempToken ===");
        u.setEnabled(true);
        u.setMfaEnabled(true);
        userRepository.save(u);
        
        ResponseEntity<Map> res9Login = restTemplate.postForEntity("/api/v1/auth/login", req2, Map.class);
        String tempToken4 = (String) res9Login.getBody().get("tempToken");
        
        u.setMfaEnabled(false);
        userRepository.save(u);
        
        MfaVerifyRequest req9 = new MfaVerifyRequest();
        req9.setTempToken(tempToken4);
        req9.setCode(generateValidTotp());
        ResponseEntity<Map> res9 = restTemplate.postForEntity("/api/v1/auth/mfa/verify", req9, Map.class);
        assertThat(res9.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res9.getBody().get("message").toString()).contains("no esta configurado");
        System.out.println("-> OK: HTTP 400 - " + res9.getBody().get("message"));

        System.out.println("\n=== 10. Evidencia de cifrado en BD ===");
        Optional<UserEntity> userInDb = userRepository.findByUsername(userWithMfaName);
        String secretDb = userInDb.get().getMfaSecret();
        assertThat(secretDb).startsWith("v1:");
        assertThat(secretDb).doesNotContain(RAW_SECRET);
        String decrypted = mfaCryptoService.decrypt(secretDb);
        assertThat(decrypted).isEqualTo(RAW_SECRET);
        System.out.println("-> OK: Secret comienza con v1: y es diferente a RAW_SECRET. Puede descifrarse correctamente.");
    }
}
