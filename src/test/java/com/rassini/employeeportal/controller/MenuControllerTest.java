package com.rassini.employeeportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rassini.employeeportal.dto.request.MenuParameterRequest;
import com.rassini.employeeportal.dto.request.MenuRequest;
import com.rassini.employeeportal.dto.response.MenuParameterResponse;
import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.entity.AppType;
import com.rassini.employeeportal.entity.AuthType;
import com.rassini.employeeportal.entity.TargetType;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.security.JwtService;
import com.rassini.employeeportal.service.MenuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MenuController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuService menuService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    public void testGetMenus_ReturnsInternalAndExternal() throws Exception {
        MenuResponse internalMenu = MenuResponse.builder()
                .id(1L)
                .code("USERS")
                .label("Usuarios")
                .route("/usuarios")
                .targetType(TargetType.INTERNO)
                .build();

        MenuResponse externalMenu = MenuResponse.builder()
                .id(2L)
                .code("PORTAL_PROV")
                .label("Portal Proveedores")
                .targetType(TargetType.EXTERNO)
                .externalUrl("https://portalproveedores.rassini.com/home")
                .resolvedUrl("https://portalproveedores.rassini.com/home?bu=1850")
                .appType(AppType.INTERNA)
                .authType(AuthType.SSO_IAM)
                .openInNewTab(true)
                .build();

        when(menuService.getMenus()).thenReturn(List.of(internalMenu, externalMenu));

        mockMvc.perform(get("/api/v1/menus")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].targetType").value("INTERNO"))
                .andExpect(jsonPath("$.data[0].route").value("/usuarios"))
                .andExpect(jsonPath("$.data[1].targetType").value("EXTERNO"))
                .andExpect(jsonPath("$.data[1].externalUrl").value("https://portalproveedores.rassini.com/home"))
                .andExpect(jsonPath("$.data[1].resolvedUrl").value("https://portalproveedores.rassini.com/home?bu=1850"))
                .andExpect(jsonPath("$.data[1].authType").value("SSO_IAM"))
                .andExpect(jsonPath("$.data[1].openInNewTab").value(true));
    }

    @Test
    @WithMockUser
    public void testCreateExternalMenu_Success() throws Exception {
        MenuParameterRequest paramReq = MenuParameterRequest.builder()
                .paramName("bu")
                .paramValue("${BUSINESS_UNIT}")
                .active(true)
                .build();

        MenuRequest request = MenuRequest.builder()
                .code("EXT_SAAS")
                .label("SaaS Externo")
                .applicationId(1L)
                .targetType(TargetType.EXTERNO)
                .externalUrl("https://tercero.com/portal")
                .openInNewTab(true)
                .appType(AppType.TERCERO)
                .authType(AuthType.NONE)
                .parameters(List.of(paramReq))
                .build();

        MenuResponse response = MenuResponse.builder()
                .id(10L)
                .code("EXT_SAAS")
                .label("SaaS Externo")
                .targetType(TargetType.EXTERNO)
                .externalUrl("https://tercero.com/portal")
                .resolvedUrl("https://tercero.com/portal?bu=1850")
                .openInNewTab(true)
                .appType(AppType.TERCERO)
                .authType(AuthType.NONE)
                .parameters(List.of(MenuParameterResponse.builder().id(1L).paramName("bu").paramValue("${BUSINESS_UNIT}").active(true).build()))
                .build();

        when(menuService.createMenu(any(MenuRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("EXT_SAAS"))
                .andExpect(jsonPath("$.data.targetType").value("EXTERNO"))
                .andExpect(jsonPath("$.data.appType").value("TERCERO"))
                .andExpect(jsonPath("$.data.parameters[0].paramName").value("bu"));
    }

    @Test
    @WithMockUser
    public void testCreateExternalMenu_ForbiddenJwtToken_ReturnsBadRequest() throws Exception {
        MenuParameterRequest forbiddenParam = MenuParameterRequest.builder()
                .paramName("token")
                .paramValue("${JWT}")
                .active(true)
                .build();

        MenuRequest request = MenuRequest.builder()
                .code("EXT_INVALID")
                .label("Inseguro")
                .applicationId(1L)
                .targetType(TargetType.EXTERNO)
                .externalUrl("https://tercero.com")
                .parameters(List.of(forbiddenParam))
                .build();

        when(menuService.createMenu(any(MenuRequest.class)))
                .thenThrow(new BusinessException("El valor del parámetro contiene la variable prohibida '${JWT}'. No se permite el transporte de credenciales ni tokens mediante query string."));

        mockMvc.perform(post("/api/v1/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
