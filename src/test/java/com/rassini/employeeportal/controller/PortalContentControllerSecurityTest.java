package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.config.CorsProperties;
import com.rassini.employeeportal.config.SecurityConfig;
import com.rassini.employeeportal.dto.response.AdminEventAttendanceSummaryResponse;
import com.rassini.employeeportal.dto.response.AudienceOptionsResponse;
import com.rassini.employeeportal.exception.GlobalExceptionHandler;
import com.rassini.employeeportal.security.CustomUserDetailsService;
import com.rassini.employeeportal.security.JwtAuthenticationFilter;
import com.rassini.employeeportal.security.JwtService;
import com.rassini.employeeportal.service.PortalContentService;
import com.rassini.employeeportal.service.PortalEventAttendanceService;
import org.springframework.data.domain.Page;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PortalContentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PortalContentControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private PortalContentService contentService;
    @MockBean private PortalEventAttendanceService attendanceService;
    @MockBean private JwtAuthenticationFilter jwtAuthFilter;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private CorsProperties corsProperties;

    @BeforeEach
    void setUp() throws Exception {
        when(corsProperties.getAllowedOrigins()).thenReturn(List.of("http://localhost:4200"));
        doAnswer(inv -> {
            FilterChain chain = inv.getArgument(2);
            chain.doFilter((ServletRequest) inv.getArgument(0), (ServletResponse) inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "employee", authorities = {"USER_SELF"})
    void audienceOptions_withoutPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/portal-content/audience-options"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "comms", authorities = {"PORTAL_CONTENT_READ"})
    void audienceOptions_withPermission_returns200() throws Exception {
        when(contentService.getAudienceOptions()).thenReturn(
                AudienceOptionsResponse.builder().businessUnits(List.of()).roles(List.of()).build());
        mockMvc.perform(get("/api/v1/portal-content/audience-options"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "employee", authorities = {"PORTAL_CONTENT_READ"})
    void eventAttendance_withoutPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/portal-content/1/attendance"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "comms", authorities = {"PORTAL_EVENT_ATTENDANCE_READ"})
    void eventAttendance_withPermission_returns200() throws Exception {
        when(attendanceService.getAdminAttendance(any(), any())).thenReturn(
                AdminEventAttendanceSummaryResponse.builder()
                        .contentId(1L).goingCount(0).notGoingCount(0).attendees(Page.empty()).build());
        mockMvc.perform(get("/api/v1/portal-content/1/attendance"))
                .andExpect(status().isOk());
    }
}
