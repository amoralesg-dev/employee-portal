package com.rassini.employeeportal.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudienceOptionsResponse {
    private List<OptionItem> businessUnits;
    private List<OptionItem> roles;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionItem {
        private Long id;
        private String code;
        private String name;
    }
}
