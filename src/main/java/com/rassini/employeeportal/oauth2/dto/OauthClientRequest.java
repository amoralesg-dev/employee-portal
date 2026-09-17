package com.rassini.employeeportal.oauth2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OauthClientRequest {

    @NotBlank(message = "El clientId es obligatorio")
    private String clientId;

    private String clientSecret;

    @NotBlank(message = "El clientName es obligatorio")
    private String clientName;

    @NotNull(message = "El applicationId es obligatorio")
    private Long applicationId;

    @Builder.Default
    private String clientAuthenticationMethods = "none";

    @Builder.Default
    private String authorizationGrantTypes = "authorization_code,refresh_token";

    @Builder.Default
    private String scopes = "openid,profile,email,roles,permissions,business_units";

    @Builder.Default
    private Boolean requireProofKey = true;

    @Builder.Default
    private Boolean requireAuthorizationConsent = false;

    @Builder.Default
    private Integer accessTokenTimeToLiveSeconds = 3600;

    @Builder.Default
    private Integer refreshTokenTimeToLiveSeconds = 86400;

    @Builder.Default
    private Boolean reuseRefreshTokens = false;

    @Builder.Default
    private Boolean enabled = true;

    private List<String> redirectUris;
    private List<String> postLogoutRedirectUris;
}
