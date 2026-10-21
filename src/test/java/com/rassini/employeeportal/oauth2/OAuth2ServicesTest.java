package com.rassini.employeeportal.oauth2;

import com.rassini.employeeportal.entity.ApplicationEntity;
import com.rassini.employeeportal.oauth2.dto.OauthClientRequest;
import com.rassini.employeeportal.oauth2.dto.OauthClientResponse;
import com.rassini.employeeportal.oauth2.entity.OauthClientEntity;
import com.rassini.employeeportal.oauth2.entity.OauthClientRedirectUriEntity;
import com.rassini.employeeportal.oauth2.repository.OauthClientRepository;
import com.rassini.employeeportal.oauth2.service.JpaRegisteredClientRepository;
import com.rassini.employeeportal.oauth2.service.OauthClientService;
import com.rassini.employeeportal.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2ServicesTest {

    @Mock
    private OauthClientRepository oauthClientRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private JpaRegisteredClientRepository registeredClientRepository;

    @InjectMocks
    private OauthClientService oauthClientService;

    private OauthClientEntity sampleEntity;
    private ApplicationEntity sampleApp;

    @BeforeEach
    void setUp() {
        sampleApp = ApplicationEntity.builder()
                .id(10L)
                .code("MS_PAGOS")
                .name("Portal Pagos")
                .build();

        sampleEntity = OauthClientEntity.builder()
                .id(1L)
                .clientId("ms-pagos-client")
                .clientName("Portal Pagos Client (SPA)")
                .application(sampleApp)
                .clientAuthenticationMethods("none")
                .authorizationGrantTypes("authorization_code,refresh_token")
                .scopes("openid,profile,email,roles,permissions,business_units")
                .requireProofKey(true)
                .requireAuthorizationConsent(false)
                .accessTokenTimeToLiveSeconds(3600)
                .refreshTokenTimeToLiveSeconds(86400)
                .reuseRefreshTokens(false)
                .enabled(true)
                .redirectUris(new ArrayList<>())
                .build();

        sampleEntity.getRedirectUris().add(OauthClientRedirectUriEntity.builder()
                .id(101L)
                .oauthClient(sampleEntity)
                .uri("http://localhost:4201/callback")
                .uriType(OauthClientRedirectUriEntity.UriType.REDIRECT)
                .build());

        sampleEntity.getRedirectUris().add(OauthClientRedirectUriEntity.builder()
                .id(102L)
                .oauthClient(sampleEntity)
                .uri("http://localhost:4201/login")
                .uriType(OauthClientRedirectUriEntity.UriType.POST_LOGOUT)
                .build());
    }

    @Test
    void testFindRegisteredClientByClientId_Success() {
        when(oauthClientRepository.findByClientId("ms-pagos-client")).thenReturn(Optional.of(sampleEntity));

        RegisteredClient registeredClient = registeredClientRepository.findByClientId("ms-pagos-client");

        assertNotNull(registeredClient);
        assertEquals("ms-pagos-client", registeredClient.getClientId());
        assertEquals("Portal Pagos Client (SPA)", registeredClient.getClientName());
        assertTrue(registeredClient.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE));
        assertTrue(registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE));
        assertTrue(registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN));
        assertTrue(registeredClient.getScopes().contains("openid"));
        assertTrue(registeredClient.getScopes().contains("business_units"));
        assertTrue(registeredClient.getRedirectUris().contains("http://localhost:4201/callback"));
        assertTrue(registeredClient.getRedirectUris().contains("http://localhost:4201/portal-cxp/callback"));
        assertTrue(registeredClient.getRedirectUris().contains("http://localhost:4200/callback"));
        assertTrue(registeredClient.getRedirectUris().contains("http://localhost:4200/portal-cxp/callback"));
        assertTrue(registeredClient.getRedirectUris().contains("https://rss-cxp-dev.rassini.com/portal-cxp/callback"));
        assertTrue(registeredClient.getPostLogoutRedirectUris().contains("http://localhost:4201/login"));
        assertTrue(registeredClient.getPostLogoutRedirectUris().contains("http://localhost:4201/logout"));
        assertTrue(registeredClient.getPostLogoutRedirectUris().contains("http://localhost:4200/logout"));
        assertTrue(registeredClient.getPostLogoutRedirectUris().contains("https://rss-cxp-dev.rassini.com/portal-cxp/logout"));
        assertTrue(registeredClient.getPostLogoutRedirectUris().contains("https://rss-cxp-dev.rassini.com/employee-portal/auth/login"));
        assertTrue(registeredClient.getRedirectUris().stream().noneMatch(uri -> uri.contains("*")));
        assertTrue(registeredClient.getPostLogoutRedirectUris().stream().noneMatch(uri -> uri.contains("*")));
        assertTrue(registeredClient.getRedirectUris().stream().noneMatch(uri -> uri.contains("10.15.1.15")));
        assertTrue(registeredClient.getClientSettings().isRequireProofKey());
    }

    @Test
    void testDevOidcConfiguration_UsesHttpsPublicIssuerAndLoginUrl() throws Exception {
        org.springframework.beans.factory.config.YamlPropertiesFactoryBean yaml = new org.springframework.beans.factory.config.YamlPropertiesFactoryBean();
        yaml.setResources(new org.springframework.core.io.ClassPathResource("application-dev.yml"));
        java.util.Properties props = yaml.getObject();
        assertNotNull(props);

        String rawIssuer = props.getProperty("app.security.oauth2.authorizationserver.issuer");
        String rawLoginUrl = props.getProperty("app.security.oauth2.authorizationserver.login-url");

        assertNotNull(rawIssuer);
        assertNotNull(rawLoginUrl);
        assertTrue(rawIssuer.contains("https://rss-cxp-dev.rassini.com/employee-portal"));
        assertFalse(rawIssuer.contains("10.15.1.15:8083"));
        assertTrue(rawLoginUrl.contains("https://rss-cxp-dev.rassini.com/employee-portal/auth/login"));

        com.rassini.employeeportal.oauth2.config.AuthorizationServerConfig config =
                new com.rassini.employeeportal.oauth2.config.AuthorizationServerConfig(null, null, null, null, null);
        org.springframework.test.util.ReflectionTestUtils.setField(
                config, "issuer", "https://rss-cxp-dev.rassini.com/employee-portal");
        org.springframework.test.util.ReflectionTestUtils.setField(
                config, "loginUrl", "https://rss-cxp-dev.rassini.com/employee-portal/auth/login");

        org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings settings =
                config.authorizationServerSettings();
        assertEquals("https://rss-cxp-dev.rassini.com/employee-portal", settings.getIssuer());
    }

    @Test
    void testFindRegisteredClientByClientId_DisabledClientReturnsNull() {
        sampleEntity.setEnabled(false);
        when(oauthClientRepository.findByClientId("ms-pagos-client")).thenReturn(Optional.of(sampleEntity));

        RegisteredClient registeredClient = registeredClientRepository.findByClientId("ms-pagos-client");

        assertNull(registeredClient);
    }

    @Test
    void testOauthClientService_FindAll() {
        when(oauthClientRepository.findAll()).thenReturn(List.of(sampleEntity));

        List<OauthClientResponse> list = oauthClientService.findAll();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("ms-pagos-client", list.get(0).getClientId());
        assertEquals("MS_PAGOS", list.get(0).getApplicationCode());
        assertEquals(1, list.get(0).getRedirectUris().size());
        assertEquals(1, list.get(0).getPostLogoutRedirectUris().size());
    }

    @Test
    void testOauthClientService_Create() {
        OauthClientRequest request = OauthClientRequest.builder()
                .clientId("new-client")
                .clientName("New Client")
                .applicationId(10L)
                .clientAuthenticationMethods("none")
                .authorizationGrantTypes("authorization_code")
                .scopes("openid,profile")
                .requireProofKey(true)
                .redirectUris(List.of("http://localhost:3000/callback"))
                .postLogoutRedirectUris(List.of("http://localhost:3000/logout"))
                .build();

        when(oauthClientRepository.existsByClientId("new-client")).thenReturn(false);
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(sampleApp));
        when(oauthClientRepository.save(any(OauthClientEntity.class))).thenAnswer(invocation -> {
            OauthClientEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            return entity;
        });

        OauthClientResponse response = oauthClientService.create(request);

        assertNotNull(response);
        assertEquals(99L, response.getId());
        assertEquals("new-client", response.getClientId());
        assertEquals(1, response.getRedirectUris().size());
        assertEquals("http://localhost:3000/callback", response.getRedirectUris().get(0));
        assertEquals(1, response.getPostLogoutRedirectUris().size());
        assertEquals("http://localhost:3000/logout", response.getPostLogoutRedirectUris().get(0));
    }
}
