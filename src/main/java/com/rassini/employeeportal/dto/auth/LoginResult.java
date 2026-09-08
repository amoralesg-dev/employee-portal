package com.rassini.employeeportal.dto.auth;

import com.rassini.employeeportal.dto.mfa.MfaPendingResponse;
import com.rassini.employeeportal.dto.response.LoginResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {
    private boolean mfaRequired;
    private MfaPendingResponse pendingResponse;
    private LoginResponse successResponse;
}
