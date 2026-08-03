package com.rassini.employeeportal.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean active;
}
