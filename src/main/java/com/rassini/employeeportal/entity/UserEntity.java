package com.rassini.employeeportal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA que mapea la tabla {@code users} del esquema {@code iam}.
 * <p>
 * IMPORTANTE: el campo {@code passwordHash} NUNCA debe exponerse en DTOs de respuesta ni en logs.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"passwordHash", "roles", "businessUnits", "mfaSecret"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "username", length = 100, unique = true, nullable = false)
    private String username;

    @Column(name = "email", length = 150, unique = true, nullable = false)
    private String email;

    /**
     * Hash de contraseña — NUNCA exponer en DTOs ni logs.
     */
    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "enabled", columnDefinition = "TINYINT(1)")
    private Boolean enabled;

    @Column(name = "force_password_change", columnDefinition = "TINYINT(1)")
    @Builder.Default
    private Boolean forcePasswordChange = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Relaciones ───────────────────────────────────────────────────────────

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_business_unit",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "business_unit_id")
    )
    @Builder.Default
    private Set<BusinessUnitEntity> businessUnits = new HashSet<>();

    @Column(name = "has_all_business_units", columnDefinition = "TINYINT(1)", nullable = false)
    @Builder.Default
    private Boolean hasAllBusinessUnits = false;

    @Column(name = "mfa_enabled", columnDefinition = "TINYINT(1)", nullable = false)
    @Builder.Default
    private Boolean mfaEnabled = false;

    @Column(name = "mfa_required", columnDefinition = "TINYINT(1)", nullable = false)
    @Builder.Default
    private Boolean mfaRequired = false;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "mfa_secret", length = 255)
    private String mfaSecret;

    @Column(name = "mfa_enabled_at")
    private LocalDateTime mfaEnabledAt;
}
