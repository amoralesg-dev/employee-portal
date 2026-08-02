package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.RoleRequest;
import com.rassini.employeeportal.dto.response.PermissionResponse;
import com.rassini.employeeportal.dto.response.RoleResponse;
import com.rassini.employeeportal.entity.PermissionEntity;
import com.rassini.employeeportal.entity.RoleEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.PermissionMapper;
import com.rassini.employeeportal.mapper.RoleMapper;
import com.rassini.employeeportal.repository.PermissionRepository;
import com.rassini.employeeportal.repository.RoleRepository;
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

class RoleServiceImplTest {
    @InjectMocks private RoleServiceImpl service;
    @Mock private RoleRepository repository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private RoleMapper mapper;
    @Mock private PermissionMapper permissionMapper;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void testGetRoles() {
        when(repository.findAll()).thenReturn(List.of(new RoleEntity()));
        when(mapper.toResponse(any())).thenReturn(new RoleResponse());
        assertFalse(service.getRoles().isEmpty());
    }

    @Test
    void testCreateRole_Success() {
        RoleRequest req = new RoleRequest(); req.setCode("R1");
        when(repository.existsByCode("R1")).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(new RoleEntity());
        when(repository.save(any())).thenReturn(new RoleEntity());
        when(mapper.toResponse(any())).thenReturn(new RoleResponse());
        assertNotNull(service.createRole(req));
    }

    @Test
    void testCreateRole_CodeExists() {
        RoleRequest req = new RoleRequest(); req.setCode("R1");
        when(repository.existsByCode("R1")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.createRole(req));
    }
    
    @Test
    void testGetRoleById_Found() {
        when(repository.findById(1L)).thenReturn(Optional.of(new RoleEntity()));
        when(mapper.toResponse(any())).thenReturn(new RoleResponse());
        assertNotNull(service.getRoleById(1L));
    }
    
    @Test
    void testGetRoleById_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getRoleById(1L));
    }

    @Test
    void testUpdateRole_Success() {
        RoleRequest req = new RoleRequest(); req.setName("n"); req.setCode("R1");
        RoleEntity ent = new RoleEntity(); ent.setCode("R1");
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(repository.save(any())).thenReturn(ent);
        when(mapper.toResponse(any())).thenReturn(new RoleResponse());
        assertNotNull(service.updateRole(1L, req));
    }

    @Test
    void testUpdateRole_CodeExists() {
        RoleRequest req = new RoleRequest(); req.setCode("R2");
        RoleEntity ent = new RoleEntity(); ent.setCode("R1");
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(repository.existsByCode("R2")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.updateRole(1L, req));
    }
    
    @Test
    void testDeleteRole_Found() {
        RoleEntity ent = new RoleEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        service.deleteRole(1L);
        verify(repository).delete(ent);
    }
    
    @Test
    void testGetRolePermissions() {
        RoleEntity ent = new RoleEntity(); ent.setPermissions(Set.of(new PermissionEntity()));
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(permissionMapper.toResponseShallow(any())).thenReturn(new PermissionResponse());
        assertFalse(service.getRolePermissions(1L).isEmpty());
    }
    
    @Test
    void testReplaceRolePermissions_Success() {
        RoleEntity ent = new RoleEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(permissionRepository.findAllById(Set.of(1L))).thenReturn(List.of(new PermissionEntity()));
        when(repository.save(any())).thenReturn(ent);
        when(mapper.toResponse(any())).thenReturn(new RoleResponse());
        assertNotNull(service.replaceRolePermissions(1L, Set.of(1L)));
    }
    
    @Test
    void testReplaceRolePermissions_NotFound() {
        RoleEntity ent = new RoleEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(permissionRepository.findAllById(Set.of(1L))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.replaceRolePermissions(1L, Set.of(1L)));
    }
}
