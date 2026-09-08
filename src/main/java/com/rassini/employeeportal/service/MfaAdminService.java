package com.rassini.employeeportal.service;

public interface MfaAdminService {
    void enableMfa(Long targetUserId, String adminUsername);
    void disableMfa(Long targetUserId, String adminUsername);
    void resetMfa(Long targetUserId, String adminUsername);
    void requireMfa(Long targetUserId, String adminUsername);
    void optionalMfa(Long targetUserId, String adminUsername);
}
