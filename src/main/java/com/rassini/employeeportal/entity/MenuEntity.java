package com.rassini.employeeportal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA que mapea la tabla {@code menus} del esquema {@code iam}.
 * <p>
 * Soporta jerarquía self-reference mediante {@code parent_id}.
 */
@Entity
@Table(name = "menus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"parent", "children", "permissions"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MenuEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "code", length = 100, unique = true, nullable = false)
    private String code;

    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "route", length = 200)
    private String route;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "order_index")
    private Integer orderIndex;

    // ─── Self-reference jerárquica ────────────────────────────────────────────

    /** Menú padre (puede ser null si es raíz). Mapea a {@code parent_id}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private MenuEntity parent;

    /** Submenús hijos que tienen a este menú como padre. */
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<MenuEntity> children = new HashSet<>();

    // ─── Relaciones ───────────────────────────────────────────────────────────

    /** Aplicación a la que pertenece el menú. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private ApplicationEntity application;

    /** Lado inverso de la relación ManyToMany con PermissionEntity. */
    @ManyToMany(mappedBy = "menus", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PermissionEntity> permissions = new HashSet<>();
}
