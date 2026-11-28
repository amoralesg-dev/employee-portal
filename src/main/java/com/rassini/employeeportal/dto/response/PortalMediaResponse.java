package com.rassini.employeeportal.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalMediaResponse {
    private Long id;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
}
