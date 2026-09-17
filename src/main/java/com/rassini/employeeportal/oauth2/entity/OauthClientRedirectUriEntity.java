package com.rassini.employeeportal.oauth2.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_client_redirect_uris")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "oauthClient")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OauthClientRedirectUriEntity {

    public enum UriType {
        REDIRECT,
        POST_LOGOUT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oauth_client_id", nullable = false)
    private OauthClientEntity oauthClient;

    @Column(name = "uri", length = 500, nullable = false)
    private String uri;

    @Enumerated(EnumType.STRING)
    @Column(name = "uri_type", nullable = false, length = 20)
    @Builder.Default
    private UriType uriType = UriType.REDIRECT;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
