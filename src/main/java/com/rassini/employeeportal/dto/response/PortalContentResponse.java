package com.rassini.employeeportal.dto.response;

import com.rassini.employeeportal.entity.PortalContentCategory;
import com.rassini.employeeportal.entity.PortalContentStatus;
import com.rassini.employeeportal.entity.PortalContentType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalContentResponse {
    private Long id;
    private PortalContentType contentType;
    private PortalContentCategory category;
    private PortalContentStatus status;
    private String displayStatus;
    private Integer priority;
    private LocalDateTime publishFrom;
    private LocalDateTime publishTo;
    private Long imageId;
    private String linkUrl;
    private Boolean linkNewTab;
    private String icon;
    private EventInfo event;
    private List<Long> businessUnitIds;
    private List<Long> roleIds;
    private List<TranslationInfo> translations;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventInfo {
        private LocalDateTime start;
        private LocalDateTime end;
        private String location;
        private String teamsUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TranslationInfo {
        private String language;
        private String title;
        private String summary;
        private String body;
        private String linkLabel;
    }
}
