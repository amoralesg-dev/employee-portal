package com.rassini.employeeportal.oauth2.service;

import com.rassini.employeeportal.entity.ApplicationEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.oauth2.dto.OauthClientRequest;
import com.rassini.employeeportal.oauth2.dto.OauthClientResponse;
import com.rassini.employeeportal.oauth2.entity.OauthClientEntity;
import com.rassini.employeeportal.oauth2.entity.OauthClientRedirectUriEntity;
import com.rassini.employeeportal.oauth2.repository.OauthClientRepository;
import com.rassini.employeeportal.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OauthClientService {

    private final OauthClientRepository oauthClientRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public List<OauthClientResponse> findAll() {
        return oauthClientRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OauthClientResponse findById(Long id) {
        return oauthClientRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("OauthClient", "id", id));
    }

    @Transactional(readOnly = true)
    public OauthClientResponse findByClientId(String clientId) {
        return oauthClientRepository.findByClientId(clientId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("OauthClient", "clientId", clientId));
    }

    @Transactional
    public OauthClientResponse create(OauthClientRequest request) {
        if (oauthClientRepository.existsByClientId(request.getClientId())) {
            throw new BusinessException("Ya existe un cliente OAuth2 con el clientId: " + request.getClientId());
        }

        ApplicationEntity application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", request.getApplicationId()));

        OauthClientEntity entity = OauthClientEntity.builder()
                .clientId(request.getClientId())
                .clientSecret(request.getClientSecret())
                .clientName(request.getClientName())
                .application(application)
                .clientAuthenticationMethods(request.getClientAuthenticationMethods())
                .authorizationGrantTypes(request.getAuthorizationGrantTypes())
                .scopes(request.getScopes())
                .requireProofKey(request.getRequireProofKey())
                .requireAuthorizationConsent(request.getRequireAuthorizationConsent())
                .accessTokenTimeToLiveSeconds(request.getAccessTokenTimeToLiveSeconds())
                .refreshTokenTimeToLiveSeconds(request.getRefreshTokenTimeToLiveSeconds())
                .reuseRefreshTokens(request.getReuseRefreshTokens())
                .enabled(request.getEnabled())
                .build();

        populateRedirectUris(entity, request.getRedirectUris(), request.getPostLogoutRedirectUris());

        OauthClientEntity saved = oauthClientRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public OauthClientResponse update(Long id, OauthClientRequest request) {
        OauthClientEntity entity = oauthClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OauthClient", "id", id));

        if (!entity.getClientId().equals(request.getClientId()) && oauthClientRepository.existsByClientId(request.getClientId())) {
            throw new BusinessException("Ya existe un cliente OAuth2 con el clientId: " + request.getClientId());
        }

        ApplicationEntity application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", request.getApplicationId()));

        entity.setClientId(request.getClientId());
        if (request.getClientSecret() != null) {
            entity.setClientSecret(request.getClientSecret());
        }
        entity.setClientName(request.getClientName());
        entity.setApplication(application);
        entity.setClientAuthenticationMethods(request.getClientAuthenticationMethods());
        entity.setAuthorizationGrantTypes(request.getAuthorizationGrantTypes());
        entity.setScopes(request.getScopes());
        entity.setRequireProofKey(request.getRequireProofKey());
        entity.setRequireAuthorizationConsent(request.getRequireAuthorizationConsent());
        entity.setAccessTokenTimeToLiveSeconds(request.getAccessTokenTimeToLiveSeconds());
        entity.setRefreshTokenTimeToLiveSeconds(request.getRefreshTokenTimeToLiveSeconds());
        entity.setReuseRefreshTokens(request.getReuseRefreshTokens());
        entity.setEnabled(request.getEnabled());

        entity.getRedirectUris().clear();
        populateRedirectUris(entity, request.getRedirectUris(), request.getPostLogoutRedirectUris());

        OauthClientEntity saved = oauthClientRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!oauthClientRepository.existsById(id)) {
            throw new ResourceNotFoundException("OauthClient", "id", id);
        }
        oauthClientRepository.deleteById(id);
    }

    private void populateRedirectUris(OauthClientEntity entity, List<String> redirectUris, List<String> postLogoutUris) {
        if (entity.getRedirectUris() == null) {
            entity.setRedirectUris(new ArrayList<>());
        }
        if (redirectUris != null) {
            for (String uri : redirectUris) {
                if (uri != null && !uri.isBlank()) {
                    entity.getRedirectUris().add(OauthClientRedirectUriEntity.builder()
                            .oauthClient(entity)
                            .uri(uri.trim())
                            .uriType(OauthClientRedirectUriEntity.UriType.REDIRECT)
                            .build());
                }
            }
        }
        if (postLogoutUris != null) {
            for (String uri : postLogoutUris) {
                if (uri != null && !uri.isBlank()) {
                    entity.getRedirectUris().add(OauthClientRedirectUriEntity.builder()
                            .oauthClient(entity)
                            .uri(uri.trim())
                            .uriType(OauthClientRedirectUriEntity.UriType.POST_LOGOUT)
                            .build());
                }
            }
        }
    }

    private OauthClientResponse toResponse(OauthClientEntity entity) {
        List<String> redirects = new ArrayList<>();
        List<String> postLogouts = new ArrayList<>();

        if (entity.getRedirectUris() != null) {
            for (OauthClientRedirectUriEntity r : entity.getRedirectUris()) {
                if (r.getUriType() == OauthClientRedirectUriEntity.UriType.REDIRECT) {
                    redirects.add(r.getUri());
                } else if (r.getUriType() == OauthClientRedirectUriEntity.UriType.POST_LOGOUT) {
                    postLogouts.add(r.getUri());
                }
            }
        }

        return OauthClientResponse.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .clientName(entity.getClientName())
                .applicationId(entity.getApplication() != null ? entity.getApplication().getId() : null)
                .applicationCode(entity.getApplication() != null ? entity.getApplication().getCode() : null)
                .applicationName(entity.getApplication() != null ? entity.getApplication().getName() : null)
                .clientAuthenticationMethods(entity.getClientAuthenticationMethods())
                .authorizationGrantTypes(entity.getAuthorizationGrantTypes())
                .scopes(entity.getScopes())
                .requireProofKey(entity.getRequireProofKey())
                .requireAuthorizationConsent(entity.getRequireAuthorizationConsent())
                .accessTokenTimeToLiveSeconds(entity.getAccessTokenTimeToLiveSeconds())
                .refreshTokenTimeToLiveSeconds(entity.getRefreshTokenTimeToLiveSeconds())
                .reuseRefreshTokens(entity.getReuseRefreshTokens())
                .enabled(entity.getEnabled())
                .redirectUris(redirects)
                .postLogoutRedirectUris(postLogouts)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
