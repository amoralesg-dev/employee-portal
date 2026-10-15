package com.rassini.employeeportal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuParameterResponse {

    private Long id;
    private String paramName;
    private String paramValue;
    private Boolean active;
}
