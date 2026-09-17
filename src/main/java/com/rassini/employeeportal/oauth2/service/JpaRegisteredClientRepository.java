package com.rassini.employeeportal.oauth2.service;

import com.rassini.employeeportal.oauth2.entity.OauthClientEntity;
import com.rassini.employeeportal.oauth2.entity.OauthClientRedirectUriEntity;
import com.rassini.employeeportal.oauth2.repository.OauthClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final OauthClientRepository oauthClientRepository;

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("Saving RegisteredClient via RegisteredClientRepository is not supported directly. Use OauthClientService.");
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findById(String id) {
        return oauthClientRepository.findById(Long.valueOf(id))
                .filter(OauthClientEntity::getEnabled)
                .map(this::toRegisteredClient)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findByClientId(String clientId) {
        return oauthClientRepository.findByClientId(clientId)
                .filter(OauthClientEntity::getEnabled)
                .map(this::toRegisteredClient)
                .orElse(null);
    }

    private RegisteredClient toRegisteredClient(OauthClientEntity entity) {
        RegisteredClient.Builder builder = RegisteredClient.withId(String.valueOf(entity.getId()))
                .clientId(entity.getClientId())
                .clientName(entity.getClientName());

        if (entity.getClientSecret() != null && !entity.getClientSecret().isBlank()) {
            builder.clientSecret(entity.getClientSecret());
        }

        if (entity.getClientAuthenticationMethods() != null) {
            Arrays.stream(entity.getClientAuthenticationMethods().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(ClientAuthenticationMethod::new)
                    .forEach(builder::clientAuthenticationMethod);
        }

        if (entity.getAuthorizationGrantTypes() != null) {
            Arrays.stream(entity.getAuthorizationGrantTypes().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(AuthorizationGrantType::new)
                    .forEach(builder::authorizationGrantType);
        }

        if (entity.getScopes() != null) {
            Arrays.stream(entity.getScopes().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(builder::scope);
        }

        if (entity.getRedirectUris() != null) {
            for (OauthClientRedirectUriEntity uriEntity : entity.getRedirectUris()) {
                if (uriEntity.getUriType() == OauthClientRedirectUriEntity.UriType.REDIRECT) {
                    builder.redirectUri(uriEntity.getUri());
                } else if (uriEntity.getUriType() == OauthClientRedirectUriEntity.UriType.POST_LOGOUT) {
                    builder.postLogoutRedirectUri(uriEntity.getUri());
                }
            }
        }

        ClientSettings clientSettings = ClientSettings.builder()
                .requireProofKey(Boolean.TRUE.equals(entity.getRequireProofKey()))
                .requireAuthorizationConsent(Boolean.TRUE.equals(entity.getRequireAuthorizationConsent()))
                .build();
        builder.clientSettings(clientSettings);

        TokenSettings tokenSettings = TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                .accessTokenTimeToLive(Duration.ofSeconds(entity.getAccessTokenTimeToLiveSeconds()))
                .refreshTokenTimeToLive(Duration.ofSeconds(entity.getRefreshTokenTimeToLiveSeconds()))
                .reuseRefreshTokens(Boolean.TRUE.equals(entity.getReuseRefreshTokens()))
                .build();
        builder.tokenSettings(tokenSettings);

        return builder.build();
    }
}
