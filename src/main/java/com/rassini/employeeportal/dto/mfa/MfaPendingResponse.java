package com.rassini.employeeportal.dto.mfa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaPendingResponse {
    @Builder.Default
    private boolean mfaRequired = true;
    private boolean mfaSetupRequired;
    private String tempToken;
    @Builder.Default
    private long expiresIn = 300;
    @Builder.Default
    private String message = "Se requiere validación de factor múltiple";
    private String qrCodeUri;
    private String manualEntryKey;
}
