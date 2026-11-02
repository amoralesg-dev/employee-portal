package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.ApplicationDto;
import com.rassini.employeeportal.entity.ApplicationEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.repository.ApplicationRepository;
import com.rassini.employeeportal.repository.MenuRepository;
import com.rassini.employeeportal.repository.PermissionRepository;
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

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private PermissionRepository permissionRepository;

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
    void create_WhenActiveNull_DefaultsToTrue() {
        ApplicationDto noActiveDto = ApplicationDto.builder().id(1L).code("APP").name("App Name").active(null).build();
        when(applicationRepository.save(any(ApplicationEntity.class))).thenReturn(entity);
        ApplicationDto result = applicationService.create(noActiveDto);
        assertNotNull(result);
    }

    @Test
    void getById_ShouldReturnDto() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(entity));
        ApplicationDto result = applicationService.getById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> applicationService.getById(99L));
    }

    @Test
    void getByCode_ShouldReturnDto() {
        when(applicationRepository.findByCode("APP")).thenReturn(Optional.of(entity));
        ApplicationDto result = applicationService.getByCode("APP");
        assertNotNull(result);
        assertEquals("APP", result.getCode());
    }

    @Test
    void getByCode_NotFound_ThrowsException() {
        when(applicationRepository.findByCode("NONE")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> applicationService.getByCode("NONE"));
    }

    @Test
    void getAll_ShouldReturnList() {
        when(applicationRepository.findAll()).thenReturn(List.of(entity));
        List<ApplicationDto> result = applicationService.getAll();
        assertFalse(result.isEmpty());
    }

    @Test
    void getActive_ShouldReturnActiveList() {
        when(applicationRepository.findByActiveTrue()).thenReturn(List.of(entity));
        List<ApplicationDto> result = applicationService.getActive();
        assertFalse(result.isEmpty());
    }

    @Test
    void update_ShouldUpdateAndReturnDto() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(applicationRepository.save(any(ApplicationEntity.class))).thenReturn(entity);
        ApplicationDto result = applicationService.update(1L, dto);
        assertNotNull(result);
        assertEquals("APP", result.getCode());
    }

    @Test
    void update_NotFound_ThrowsException() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> applicationService.update(99L, dto));
    }

    @Test
    void delete_WhenNoAssociations_DeletesSuccessfully() {
        when(menuRepository.existsByApplicationId(1L)).thenReturn(false);
        when(permissionRepository.existsByApplicationId(1L)).thenReturn(false);
        applicationService.delete(1L);
        verify(applicationRepository).deleteById(1L);
    }

    @Test
    void delete_WhenMenusExist_ThrowsBusinessException() {
        when(menuRepository.existsByApplicationId(1L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> applicationService.delete(1L));
    }

    @Test
    void delete_WhenPermissionsExist_ThrowsBusinessException() {
        when(menuRepository.existsByApplicationId(1L)).thenReturn(false);
        when(permissionRepository.existsByApplicationId(1L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> applicationService.delete(1L));
    }
}
