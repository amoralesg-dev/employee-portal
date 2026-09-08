package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.request.LoginRequest;
import com.rassini.employeeportal.dto.response.LoginResponse;

public interface AuthService {
    com.rassini.employeeportal.dto.auth.LoginResult login(LoginRequest request);
    com.rassini.employeeportal.dto.response.MeResponse getMe(String username);
    void resetPassword(com.rassini.employeeportal.dto.request.ResetPasswordRequest request);
    void changePassword(String username, com.rassini.employeeportal.dto.request.ChangePasswordRequest request);
    LoginResponse refresh(com.rassini.employeeportal.dto.request.RefreshTokenRequest request);
    
    com.rassini.employeeportal.dto.mfa.MfaSetupResponse setupMfa(String username);
    void activateMfa(String username, com.rassini.employeeportal.dto.mfa.MfaActivateRequest request);
    LoginResponse verifyMfa(com.rassini.employeeportal.dto.mfa.MfaVerifyRequest request);
    void disableMfa(String username, com.rassini.employeeportal.dto.mfa.MfaDisableRequest request);
}
