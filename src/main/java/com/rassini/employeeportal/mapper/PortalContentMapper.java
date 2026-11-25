package com.rassini.employeeportal.mapper;

import com.rassini.employeeportal.dto.response.HomeItemResponse;
import com.rassini.employeeportal.dto.response.PortalContentResponse;
import com.rassini.employeeportal.dto.response.PortalMediaResponse;
import com.rassini.employeeportal.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Component
public class PortalContentMapper {

    public PortalMediaResponse toMediaResponse(PortalMediaEntity entity) {
        return PortalMediaResponse.builder()
                .id(entity.getId())
                .originalName(entity.getOriginalName())
                .contentType(entity.getContentType())
                .sizeBytes(entity.getSizeBytes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public PortalContentResponse toAdminResponse(PortalContentEntity entity, LocalDateTime now) {
        PortalContentResponse.EventInfo eventInfo = null;
        if (entity.getContentType() == PortalContentType.EVENT
                || entity.getEventStart() != null
                || entity.getEventLocation() != null
                || entity.getTeamsUrl() != null) {
            eventInfo = PortalContentResponse.EventInfo.builder()
                    .start(entity.getEventStart())
                    .end(entity.getEventEnd())
                    .location(entity.getEventLocation())
                    .teamsUrl(entity.getTeamsUrl())
                    .build();
        }

        List<PortalContentResponse.TranslationInfo> translations = entity.getTranslations().stream()
                .sorted(Comparator.comparing(PortalContentTranslationEntity::getLanguage))
                .map(t -> PortalContentResponse.TranslationInfo.builder()
                        .language(t.getLanguage())
                        .title(t.getTitle())
                        .summary(t.getSummary())
                        .body(t.getBody())
                        .linkLabel(t.getLinkLabel())
                        .build())
                .toList();

        List<Long> buIds = entity.getBusinessUnits().stream()
                .map(BusinessUnitEntity::getId)
                .sorted()
                .toList();

        List<Long> roleIds = entity.getRoles().stream()
                .map(RoleEntity::getId)
                .sorted()
                .toList();

        return PortalContentResponse.builder()
                .id(entity.getId())
                .contentType(entity.getContentType())
                .category(entity.getCategory())
                .status(entity.getStatus())
                .displayStatus(computeDisplayStatus(entity, now))
                .priority(entity.getPriority())
                .publishFrom(entity.getPublishFrom())
                .publishTo(entity.getPublishTo())
                .imageId(entity.getImage() != null ? entity.getImage().getId() : null)
                .linkUrl(entity.getLinkUrl())
                .linkNewTab(Boolean.TRUE.equals(entity.getLinkNewTab()))
                .icon(entity.getIcon())
                .event(eventInfo)
                .businessUnitIds(buIds)
                .roleIds(roleIds)
                .translations(translations)
                .createdBy(entity.getCreatedBy() != null ? entity.getCreatedBy().getUsername() : null)
                .updatedBy(entity.getUpdatedBy() != null ? entity.getUpdatedBy().getUsername() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public String computeDisplayStatus(PortalContentEntity entity, LocalDateTime now) {
        if (entity.getStatus() != PortalContentStatus.PUBLISHED) {
            return entity.getStatus().name();
        }
        if (entity.getPublishFrom() != null && entity.getPublishFrom().isAfter(now)) {
            return "SCHEDULED";
        }
        if (entity.getPublishTo() != null && !entity.getPublishTo().isAfter(now)) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }

    public HomeItemResponse toHomeItem(PortalContentEntity entity, PortalContentTranslationEntity translation, boolean includeBody) {
        HomeItemResponse.LinkInfo linkInfo = null;
        if (entity.getLinkUrl() != null && !entity.getLinkUrl().isBlank()) {
            linkInfo = HomeItemResponse.LinkInfo.builder()
                    .url(entity.getLinkUrl())
                    .label(translation.getLinkLabel())
                    .newTab(Boolean.TRUE.equals(entity.getLinkNewTab()))
                    .build();
        }

        HomeItemResponse.EventInfo eventInfo = null;
        if (entity.getContentType() == PortalContentType.EVENT) {
            eventInfo = HomeItemResponse.EventInfo.builder()
                    .start(entity.getEventStart())
                    .end(entity.getEventEnd())
                    .location(entity.getEventLocation())
                    .teamsUrl(entity.getTeamsUrl())
                    .build();
        }

        boolean hasBody = translation.getBody() != null && !translation.getBody().isBlank();

        return HomeItemResponse.builder()
                .id(entity.getId())
                .type(entity.getContentType())
                .category(entity.getCategory())
                .language(translation.getLanguage())
                .title(translation.getTitle())
                .summary(translation.getSummary())
                .body(includeBody ? translation.getBody() : null)
                .hasBody(hasBody)
                .imageId(entity.getImage() != null ? entity.getImage().getId() : null)
                .link(linkInfo)
                .icon(entity.getIcon())
                .event(eventInfo)
                .publishFrom(entity.getPublishFrom())
                .build();
    }
}
