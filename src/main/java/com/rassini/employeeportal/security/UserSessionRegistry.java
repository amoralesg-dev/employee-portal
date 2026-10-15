package com.rassini.employeeportal.security;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSession;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Registro en memoria de sesiones HTTP activas, indexadas por nombre de usuario.
 * Permite invalidar la sesión de un usuario específico durante el logout OIDC,
 * incluso cuando el logoutResponseHandler no tiene acceso directo a la HttpSession
 * (porque el AS procesa el logout vía id_token_hint sin cargar la sesión).
 */
@Component
public class UserSessionRegistry {

    private final Map<String, HttpSession> userSessions = new ConcurrentHashMap<>();

    /**
     * Registra la sesión HTTP activa para el usuario.
     * Llamar desde AuthController.login() y AuthController.verifyMfa()
     * DESPUÉS de que Spring haya creado la sesión.
     */
    public void register(String username, HttpSession session) {
        if (username != null && session != null) {
            userSessions.put(username, session);
        }
    }

    /**
     * Invalida la sesión HTTP del usuario dado y la elimina del registro.
     * Llamar desde el logoutResponseHandler del Authorization Server.
     * Es seguro llamar aunque el usuario no tenga sesión registrada.
     */
    public void invalidateSession(String username) {
        if (username == null) return;
        HttpSession session = userSessions.remove(username);
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException e) {
                // La sesión ya fue invalidada por otro medio; ignorar.
            }
        }
    }

    /**
     * Elimina el registro sin invalidar (para limpieza interna).
     */
    public void unregister(String username) {
        userSessions.remove(username);
    }
}
