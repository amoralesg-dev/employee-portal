package com.rassini.employeeportal.oauth2.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_jwk_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "privateKeyEncrypted")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OauthJwkKeyEntity {

    public enum KeyStatus {
        ACTIVE,
        ROTATING,
        RETIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "key_id", length = 100, nullable = false, unique = true)
    private String keyId;

    @Column(name = "key_type", length = 20, nullable = false)
    @Builder.Default
    private String keyType = "RSA";

    @Column(name = "algorithm", length = 20, nullable = false)
    @Builder.Default
    private String algorithm = "RS256";

    @Column(name = "use_type", length = 20, nullable = false)
    @Builder.Default
    private String useType = "sig";

    @Column(name = "public_key_pem", columnDefinition = "TEXT", nullable = false)
    private String publicKeyPem;

    @Column(name = "private_key_encrypted", columnDefinition = "TEXT", nullable = false)
    private String privateKeyEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private KeyStatus status = KeyStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
