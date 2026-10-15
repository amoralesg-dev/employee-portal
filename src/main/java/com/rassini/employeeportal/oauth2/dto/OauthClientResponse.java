package com.rassini.employeeportal.oauth2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OauthClientResponse {

    private Long id;
    private String clientId;
    private String clientName;
    private Long applicationId;
    private String applicationCode;
    private String applicationName;
    private String clientAuthenticationMethods;
    private String authorizationGrantTypes;
    private String scopes;
    private Boolean requireProofKey;
    private Boolean requireAuthorizationConsent;
    private Integer accessTokenTimeToLiveSeconds;
    private Integer refreshTokenTimeToLiveSeconds;
    private Boolean reuseRefreshTokens;
    private Boolean enabled;
    private List<String> redirectUris;
    private List<String> postLogoutRedirectUris;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
