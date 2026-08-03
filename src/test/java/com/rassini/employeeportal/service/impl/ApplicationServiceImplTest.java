package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.ApplicationDto;
import com.rassini.employeeportal.entity.ApplicationEntity;
import com.rassini.employeeportal.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private ApplicationEntity entity;
    private ApplicationDto dto;

    @BeforeEach
    void setUp() {
        entity = ApplicationEntity.builder().id(1L).code("APP").name("App Name").active(true).build();
        dto = ApplicationDto.builder().id(1L).code("APP").name("App Name").active(true).build();
    }

    @Test
    void create_ShouldReturnSavedDto() {
        when(applicationRepository.save(any(ApplicationEntity.class))).thenReturn(entity);
        ApplicationDto result = applicationService.create(dto);
        assertNotNull(result);
        assertEquals("APP", result.getCode());
    }

    @Test
    void getById_ShouldReturnDto() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(entity));
        ApplicationDto result = applicationService.getById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getAll_ShouldReturnList() {
        when(applicationRepository.findAll()).thenReturn(List.of(entity));
        List<ApplicationDto> result = applicationService.getAll();
        assertFalse(result.isEmpty());
    }
}
