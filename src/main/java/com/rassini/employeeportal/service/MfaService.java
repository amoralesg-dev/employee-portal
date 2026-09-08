package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.mfa.MfaSetupResponse;
import com.rassini.employeeportal.entity.UserEntity;

public interface MfaService {
    MfaSetupResponse generateSetup(Long userId, String email);
    void activateMfa(Long userId, String code);
    UserEntity verifyMfa(Long userId, String code, String jti, String tokenType);
    void disableMfa(Long userId, String password, String code);
}
