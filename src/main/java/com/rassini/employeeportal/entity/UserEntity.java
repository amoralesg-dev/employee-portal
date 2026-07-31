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
@ToString(exclude = {"passwordHash", "roles"})
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
}
