package com.rassini.employeeportal.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class AssignBusinessUnitsRequest {
    @NotNull(message = "Business unit IDs cannot be null")
    private Set<Long> businessUnitIds;
}
