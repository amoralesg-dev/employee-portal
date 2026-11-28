package com.rassini.employeeportal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "portal_content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"image", "translations", "businessUnits", "roles", "createdBy", "updatedBy"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PortalContentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 30, nullable = false)
    private PortalContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private PortalContentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 15, nullable = false)
    @Builder.Default
    private PortalContentStatus status = PortalContentStatus.DRAFT;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 100;

    @Column(name = "publish_from")
    private LocalDateTime publishFrom;

    @Column(name = "publish_to")
    private LocalDateTime publishTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private PortalMediaEntity image;

    @Column(name = "link_url", length = 2000)
    private String linkUrl;

    @Column(name = "link_new_tab", nullable = false)
    @Builder.Default
    private Boolean linkNewTab = false;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "event_start")
    private LocalDateTime eventStart;

    @Column(name = "event_end")
    private LocalDateTime eventEnd;

    @Column(name = "event_location", length = 200)
    private String eventLocation;

    @Column(name = "teams_url", length = 2000)
    private String teamsUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private UserEntity updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "portalContent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PortalContentTranslationEntity> translations = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "portal_content_business_units",
            joinColumns = @JoinColumn(name = "portal_content_id"),
            inverseJoinColumns = @JoinColumn(name = "business_unit_id")
    )
    @Builder.Default
    private Set<BusinessUnitEntity> businessUnits = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "portal_content_roles",
            joinColumns = @JoinColumn(name = "portal_content_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PortalContentStatus.DRAFT;
        }
        if (priority == null) {
            priority = 100;
        }
        if (linkNewTab == null) {
            linkNewTab = false;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
