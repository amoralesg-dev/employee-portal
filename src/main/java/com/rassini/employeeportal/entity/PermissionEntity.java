package com.rassini.employeeportal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA que mapea la tabla {@code permissions} del esquema {@code iam}.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"roles", "menus"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "code", length = 100, unique = true, nullable = false)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ─── Relaciones ───────────────────────────────────────────────────────────

    /** Aplicación a la que pertenece el permiso. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private ApplicationEntity application;

    /** Lado inverso de la relación ManyToMany con RoleEntity. */
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();

    /** Lado propietario de la relación ManyToMany con MenuEntity. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "permission_menu",
            joinColumns = @JoinColumn(name = "permission_id"),
            inverseJoinColumns = @JoinColumn(name = "menu_id")
    )
    @Builder.Default
    private Set<MenuEntity> menus = new HashSet<>();
}
