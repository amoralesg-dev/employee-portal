package com.rassini.employeeportal.dto.response;

import lombok.*;

/**
 * DTO que representa un placeholder soportado para parámetros de contexto en menús.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceholderResponse {

    private String code;
    private String placeholder;
    private String description;
}
