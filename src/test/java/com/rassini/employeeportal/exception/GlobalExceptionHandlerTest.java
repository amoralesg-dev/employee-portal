package com.rassini.employeeportal.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rassini.employeeportal.controller.AuthController;
import com.rassini.employeeportal.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración del GlobalExceptionHandler usando @WebMvcTest.
 * Valida que cada excepción de autenticación produzca el HTTP status correcto
 * y una respuesta JSON uniforme sin exponer detalles del servidor.
 *
 * Se excluye SecurityAutoConfiguration para evitar que CSRF/autenticación
 * intercepten las peticiones antes de que lleguen al GlobalExceptionHandler.
 * El objetivo del test es el mapeo excepción→status, no la cadena de seguridad.
 */
@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private static final String LOGIN_URL  = "/api/v1/auth/login";
    private static final String VALID_BODY = """
            {"username":"u","password":"p"}
            """;

    // ─── 401 — BadCredentialsException ───────────────────────────────────────

    @Test
    @DisplayName("POST /login con password incorrecto → 401 con mensaje seguro")
    void login_BadCredentials_Returns401() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ─── 401 — UsernameNotFoundException ─────────────────────────────────────

    @Test
    @DisplayName("POST /login con usuario inexistente → 401 (no revela si el usuario existe)")
    void login_UserNotFound_Returns401() throws Exception {
        when(authService.login(any())).thenThrow(new UsernameNotFoundException("not found"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
    }

    // ─── 403 — DisabledException ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /login con usuario deshabilitado → 403")
    void login_DisabledUser_Returns403() throws Exception {
        when(authService.login(any())).thenThrow(new DisabledException("disabled"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("La cuenta de usuario está deshabilitada"));
    }

    // ─── 500 — No expone stacktrace ───────────────────────────────────────────

    @Test
    @DisplayName("POST /login con excepción inesperada → 500 sin stacktrace")
    void login_UnexpectedException_Returns500WithoutStacktrace() throws Exception {
        when(authService.login(any())).thenThrow(new RuntimeException("internal detail"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                // Verificar que el detalle interno NO está expuesto
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not("internal detail")));
    }
}
