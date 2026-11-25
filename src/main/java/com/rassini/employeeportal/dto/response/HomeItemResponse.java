package com.rassini.employeeportal.dto.response;

import com.rassini.employeeportal.entity.PortalContentCategory;
import com.rassini.employeeportal.entity.PortalContentType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeItemResponse {
    private Long id;
    private PortalContentType type;
    private PortalContentCategory category;
    private String language;
    private String title;
    private String summary;
    private String body;
    private boolean hasBody;
    private Long imageId;
    private LinkInfo link;
    private String icon;
    private EventInfo event;
    private LocalDateTime publishFrom;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LinkInfo {
        private String url;
        private String label;
        private boolean newTab;
    }

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
}
