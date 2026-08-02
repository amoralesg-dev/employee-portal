package com.rassini.employeeportal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimpleMessageResponse {
    private int status;
    private String message;
}
