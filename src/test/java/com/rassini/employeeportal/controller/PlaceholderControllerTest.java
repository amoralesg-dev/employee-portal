package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.dto.response.PlaceholderResponse;
import com.rassini.employeeportal.security.JwtService;
import com.rassini.employeeportal.service.MenuUrlResolverService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlaceholderController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlaceholderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuUrlResolverService menuUrlResolverService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("GET /api/v1/placeholders debe retornar lista de placeholders con status 200")
    void testGetPlaceholders() throws Exception {
        List<PlaceholderResponse> mockPlaceholders = List.of(
                PlaceholderResponse.builder()
                        .code("BUSINESS_UNIT")
                        .placeholder("${BUSINESS_UNIT}")
                        .description("Código de la primera Unidad de Negocio asignada al usuario (ej. 1850)")
                        .build(),
                PlaceholderResponse.builder()
                        .code("BUSINESS_UNITS")
                        .placeholder("${BUSINESS_UNITS}")
                        .description("Códigos de todas las Unidades de Negocio asignadas separados por comas (ej. 0111,1850)")
                        .build(),
                PlaceholderResponse.builder()
                        .code("USERNAME")
                        .placeholder("${USERNAME}")
                        .description("Nombre de usuario autenticado en el portal")
                        .build()
        );

        given(menuUrlResolverService.getAvailablePlaceholders()).willReturn(mockPlaceholders);

        mockMvc.perform(get("/api/v1/placeholders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("Placeholders obtenidos exitosamente")))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].code", is("BUSINESS_UNIT")))
                .andExpect(jsonPath("$.data[0].placeholder", is("${BUSINESS_UNIT}")))
                .andExpect(jsonPath("$.data[0].description", notNullValue()))
                .andExpect(jsonPath("$.data[1].code", is("BUSINESS_UNITS")))
                .andExpect(jsonPath("$.data[1].placeholder", is("${BUSINESS_UNITS}")))
                .andExpect(jsonPath("$.data[2].code", is("USERNAME")));
    }
}