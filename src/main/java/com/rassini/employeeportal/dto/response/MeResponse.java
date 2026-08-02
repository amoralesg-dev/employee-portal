package com.rassini.employeeportal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeResponse {
    private AuthenticatedUserResponse user;
    private Object roles;
    private Object permissions;
    private Object menus;
}
