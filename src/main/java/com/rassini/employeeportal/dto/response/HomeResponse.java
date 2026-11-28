package com.rassini.employeeportal.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeResponse {
    private String requestedLanguage;
    private LocalDateTime generatedAt;
    private HomeItemResponse hero;
    private List<HomeItemResponse> secondaryNews;
    private List<HomeItemResponse> campaigns;
    private List<HomeItemResponse> weeklyInfo;
    private List<HomeItemResponse> events;
    private List<HomeItemResponse> quickAccess;
    private List<HomeItemResponse> notices;
}
