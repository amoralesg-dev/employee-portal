package com.rassini.employeeportal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "portal_content_translation",
        uniqueConstraints = @UniqueConstraint(name = "uk_portal_content_translation", columnNames = {"portal_content_id", "language"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "portalContent")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PortalContentTranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portal_content_id", nullable = false)
    private PortalContentEntity portalContent;

    @EqualsAndHashCode.Include
    @Column(name = "language", length = 5, nullable = false)
    private String language;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "link_label", length = 100)
    private String linkLabel;
}
