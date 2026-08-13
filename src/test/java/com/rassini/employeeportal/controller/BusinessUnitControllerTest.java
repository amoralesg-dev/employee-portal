package com.rassini.employeeportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rassini.employeeportal.dto.request.BusinessUnitRequest;
import com.rassini.employeeportal.dto.response.BusinessUnitResponse;
import com.rassini.employeeportal.service.BusinessUnitService;
import com.rassini.employeeportal.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BusinessUnitController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple controller test
public class BusinessUnitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BusinessUnitService businessUnitService;

    @MockBean
    private JwtService jwtService; // Need this to satisfy security config if any

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    public void testGetAllBusinessUnits() throws Exception {
        BusinessUnitResponse response = BusinessUnitResponse.builder().id(1L).code("BU1").name("Unit 1").build();
        when(businessUnitService.getAllBusinessUnits()).thenReturn(Collections.singletonList(response));

        mockMvc.perform(get("/api/v1/business-units")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BU1"));
    }

    @Test
    @WithMockUser
    public void testCreateBusinessUnit() throws Exception {
        BusinessUnitRequest request = new BusinessUnitRequest("BU2", "Unit 2", null, true);
        BusinessUnitResponse response = BusinessUnitResponse.builder().id(2L).code("BU2").name("Unit 2").build();
        
        when(businessUnitService.createBusinessUnit(any(BusinessUnitRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/business-units")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BU2"));
    }
}
