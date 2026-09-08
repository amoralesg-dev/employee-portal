package com.rassini.employeeportal.exception;

public class InvalidMfaSessionException extends RuntimeException {
    private final String code;
    public InvalidMfaSessionException(String code, String message) {
        super(message);
        this.code = code;
    }
    public String getCode() { return code; }
}