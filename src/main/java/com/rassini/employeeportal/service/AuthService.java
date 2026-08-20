package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.request.LoginRequest;
import com.rassini.employeeportal.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    com.rassini.employeeportal.dto.response.MeResponse getMe(String username);
    void resetPassword(com.rassini.employeeportal.dto.request.ResetPasswordRequest request);
    void changePassword(String username, com.rassini.employeeportal.dto.request.ChangePasswordRequest request);
    LoginResponse refresh(com.rassini.employeeportal.dto.request.RefreshTokenRequest request);
}
