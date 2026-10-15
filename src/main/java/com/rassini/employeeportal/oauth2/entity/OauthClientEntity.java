package com.rassini.employeeportal.oauth2.entity;

import com.rassini.employeeportal.entity.ApplicationEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "oauth_clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"redirectUris", "application"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OauthClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "client_id", length = 100, nullable = false, unique = true)
    private String clientId;

    @Column(name = "client_secret", length = 255)
    private String clientSecret;

    @Column(name = "client_name", length = 150, nullable = false)
    private String clientName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private ApplicationEntity application;

    @Column(name = "client_authentication_methods", length = 255, nullable = false)
    @Builder.Default
    private String clientAuthenticationMethods = "none";

    @Column(name = "authorization_grant_types", length = 255, nullable = false)
    @Builder.Default
    private String authorizationGrantTypes = "authorization_code,refresh_token";

    @Column(name = "scopes", length = 500, nullable = false)
    @Builder.Default
    private String scopes = "openid,profile,email,roles,permissions,business_units";

    @Column(name = "require_proof_key", nullable = false)
    @Builder.Default
    private Boolean requireProofKey = true;

    @Column(name = "require_authorization_consent", nullable = false)
    @Builder.Default
    private Boolean requireAuthorizationConsent = false;

    @Column(name = "access_token_time_to_live_seconds", nullable = false)
    @Builder.Default
    private Integer accessTokenTimeToLiveSeconds = 3600;

    @Column(name = "refresh_token_time_to_live_seconds", nullable = false)
    @Builder.Default
    private Integer refreshTokenTimeToLiveSeconds = 86400;

    @Column(name = "reuse_refresh_tokens", nullable = false)
    @Builder.Default
    private Boolean reuseRefreshTokens = false;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @OneToMany(mappedBy = "oauthClient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OauthClientRedirectUriEntity> redirectUris = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
