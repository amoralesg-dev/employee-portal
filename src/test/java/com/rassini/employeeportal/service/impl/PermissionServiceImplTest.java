package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.PermissionRequest;
import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.dto.response.PermissionResponse;
import com.rassini.employeeportal.entity.MenuEntity;
import com.rassini.employeeportal.entity.PermissionEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.MenuMapper;
import com.rassini.employeeportal.mapper.PermissionMapper;
import com.rassini.employeeportal.repository.MenuRepository;
import com.rassini.employeeportal.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionServiceImplTest {
    @InjectMocks private PermissionServiceImpl service;
    @Mock private PermissionRepository repository;
    @Mock private MenuRepository menuRepository;
    @Mock private PermissionMapper mapper;
    @Mock private MenuMapper menuMapper;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void testGetPermissions() {
        when(repository.findAll()).thenReturn(List.of(new PermissionEntity()));
        when(mapper.toResponse(any())).thenReturn(new PermissionResponse());
        assertFalse(service.getPermissions().isEmpty());
    }

    @Test
    void testCreatePermission_Success() {
        PermissionRequest req = new PermissionRequest(); req.setCode("P1");
        when(repository.existsByCode("P1")).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(new PermissionEntity());
        when(repository.save(any())).thenReturn(new PermissionEntity());
        when(mapper.toResponse(any())).thenReturn(new PermissionResponse());
        assertNotNull(service.createPermission(req));
    }

    @Test
    void testCreatePermission_CodeExists() {
        PermissionRequest req = new PermissionRequest(); req.setCode("P1");
        when(repository.existsByCode("P1")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.createPermission(req));
    }
    
    @Test
    void testGetPermissionById_Found() {
        when(repository.findById(1L)).thenReturn(Optional.of(new PermissionEntity()));
        when(mapper.toResponse(any())).thenReturn(new PermissionResponse());
        assertNotNull(service.getPermissionById(1L));
    }
    
    @Test
    void testGetPermissionById_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getPermissionById(1L));
    }
    
    @Test
    void testUpdatePermission_Success() {
        PermissionRequest req = new PermissionRequest(); req.setDescription("d"); req.setCode("P1");
        PermissionEntity ent = new PermissionEntity(); ent.setCode("P1");
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(repository.save(any())).thenReturn(ent);
        when(mapper.toResponse(any())).thenReturn(new PermissionResponse());
        assertNotNull(service.updatePermission(1L, req));
    }

    @Test
    void testUpdatePermission_CodeExists() {
        PermissionRequest req = new PermissionRequest(); req.setCode("P2");
        PermissionEntity ent = new PermissionEntity(); ent.setCode("P1");
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(repository.existsByCode("P2")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.updatePermission(1L, req));
    }
    
    @Test
    void testDeletePermission_Found() {
        PermissionEntity ent = new PermissionEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        service.deletePermission(1L);
        verify(repository).delete(ent);
    }

    @Test
    void testGetPermissionMenus() {
        PermissionEntity ent = new PermissionEntity(); ent.setMenus(Set.of(new MenuEntity()));
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(menuMapper.toResponseShallow(any())).thenReturn(new MenuResponse());
        assertFalse(service.getPermissionMenus(1L).isEmpty());
    }

    @Test
    void testReplacePermissionMenus_Success() {
        PermissionEntity ent = new PermissionEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(menuRepository.findAllById(Set.of(1L))).thenReturn(List.of(new MenuEntity()));
        when(repository.save(any())).thenReturn(ent);
        when(mapper.toResponse(any())).thenReturn(new PermissionResponse());
        assertNotNull(service.replacePermissionMenus(1L, Set.of(1L)));
    }
    
    @Test
    void testReplacePermissionMenus_NotFound() {
        PermissionEntity ent = new PermissionEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(menuRepository.findAllById(Set.of(1L))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.replacePermissionMenus(1L, Set.of(1L)));
    }
}
