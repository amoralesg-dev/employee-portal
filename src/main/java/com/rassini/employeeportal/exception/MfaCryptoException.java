package com.rassini.employeeportal.exception;

public class MfaCryptoException extends RuntimeException {
    public MfaCryptoException(String message) {
        super(message);
    }

    public MfaCryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
