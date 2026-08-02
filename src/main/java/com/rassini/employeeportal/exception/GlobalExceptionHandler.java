package com.rassini.employeeportal.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manejador global de excepciones para la API REST.
 * <p>
 * Centraliza el manejo de errores y garantiza respuestas consistentes
 * con la estructura {@link ApiErrorResponse}.
 * <p>
 * Jerarquía de handlers de autenticación:
 * <ul>
 *   <li>{@link BadCredentialsException}    → 401 (password incorrecto)</li>
 *   <li>{@link UsernameNotFoundException}  → 401 (usuario no existe)</li>
 *   <li>{@link DisabledException}          → 403 (usuario deshabilitado)</li>
 *   <li>{@link LockedException}            → 403 (usuario bloqueado)</li>
 *   <li>{@link AuthenticationException}    → 401 (cualquier otro fallo de auth)</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── Mensajes públicos seguros ────────────────────────────────────────────
    // Se usa el mismo mensaje para usuario inexistente y password incorrecto
    // para no revelar si el usuario existe (security best practice).
    private static final String MSG_BAD_CREDENTIALS = "Usuario o contraseña incorrectos";
    private static final String MSG_DISABLED        = "La cuenta de usuario está deshabilitada";
    private static final String MSG_LOCKED          = "La cuenta de usuario está bloqueada";
    private static final String MSG_AUTH_ERROR      = "Error de autenticación";
    private static final String MSG_INTERNAL        = "Error interno del servidor";

    // ─── Handlers de Spring Security ─────────────────────────────────────────

    /**
     * {@link BadCredentialsException} → 401
     * Se lanza cuando el password no coincide.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("[AUTH] BadCredentialsException: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, MSG_BAD_CREDENTIALS);
    }

    /**
     * {@link UsernameNotFoundException} → 401
     * Se lanza cuando el usuario no existe en la base de datos.
     * Se devuelve 401 (mismo mensaje que bad credentials) por seguridad:
     * no se revela si el usuario existe o no.
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        log.warn("[AUTH] UsernameNotFoundException: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, MSG_BAD_CREDENTIALS);
    }

    /**
     * {@link DisabledException} → 403
     * Se lanza cuando el usuario existe pero está deshabilitado (enabled = false).
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(DisabledException ex) {
        log.warn("[AUTH] DisabledException: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, MSG_DISABLED);
    }

    /**
     * {@link LockedException} → 403
     * Se lanza cuando la cuenta está bloqueada.
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiErrorResponse> handleLocked(LockedException ex) {
        log.warn("[AUTH] LockedException: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, MSG_LOCKED);
    }

    /**
     * {@link AuthenticationException} (base) → 401
     * Captura cualquier otra excepción de autenticación no contemplada arriba.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("[AUTH] AuthenticationException: {} — {}", ex.getClass().getSimpleName(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, MSG_AUTH_ERROR);
    }

    // ─── Handlers de negocio ─────────────────────────────────────────────────

    /**
     * {@link ResourceNotFoundException} → 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("[BUSINESS] ResourceNotFoundException: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * {@link BusinessException} → 400
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("[BUSINESS] BusinessException: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * {@link MethodArgumentNotValidException} → 400
     * Extrae los errores de validación por campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ApiErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Error de validación")
                .timestamp(LocalDateTime.now())
                .errors(fieldErrors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * {@link Exception} fallback → 500
     * No expone stacktrace ni detalles internos al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("[ERROR] Excepción no controlada: {} — {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, MSG_INTERNAL);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
