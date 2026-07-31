package com.rassini.employeeportal.exception;

/**
 * Excepción lanzada cuando se viola una regla de negocio.
 * <p>
 * Genera una respuesta HTTP 400 a través del {@code GlobalExceptionHandler}.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
