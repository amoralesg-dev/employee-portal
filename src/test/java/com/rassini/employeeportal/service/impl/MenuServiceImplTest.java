package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.MenuRequest;
import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.entity.MenuEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.MenuMapper;
import com.rassini.employeeportal.repository.MenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MenuServiceImplTest {
    @InjectMocks private MenuServiceImpl service;
    @Mock private MenuRepository repository;
    @Mock private MenuMapper mapper;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void testGetMenus() {
        when(repository.findAll()).thenReturn(List.of(new MenuEntity()));
        when(mapper.toResponseShallow(any())).thenReturn(new MenuResponse());
        assertFalse(service.getMenus().isEmpty());
    }

    @Test
    void testCreateMenu_Success() {
        MenuRequest req = new MenuRequest(); req.setCode("M1"); req.setParentId(null);
        when(repository.findByCode("M1")).thenReturn(Optional.empty());
        when(mapper.toEntity(any())).thenReturn(new MenuEntity());
        when(repository.save(any())).thenReturn(new MenuEntity());
        when(mapper.toResponse(any())).thenReturn(new MenuResponse());
        assertNotNull(service.createMenu(req));
    }
    
    @Test
    void testCreateMenu_ParentExists() {
        MenuRequest req = new MenuRequest(); req.setCode("M1"); req.setParentId(2L);
        when(repository.findByCode("M1")).thenReturn(Optional.empty());
        when(mapper.toEntity(any())).thenReturn(new MenuEntity());
        when(repository.findById(2L)).thenReturn(Optional.of(new MenuEntity()));
        when(repository.save(any())).thenReturn(new MenuEntity());
        when(mapper.toResponse(any())).thenReturn(new MenuResponse());
        assertNotNull(service.createMenu(req));
    }

    @Test
    void testCreateMenu_CodeExists() {
        MenuRequest req = new MenuRequest(); req.setCode("M1");
        when(repository.findByCode("M1")).thenReturn(Optional.of(new MenuEntity()));
        assertThrows(BusinessException.class, () -> service.createMenu(req));
    }

    @Test
    void testCreateMenu_ParentNotFound() {
        MenuRequest req = new MenuRequest(); req.setCode("M1"); req.setParentId(2L);
        when(repository.findByCode("M1")).thenReturn(Optional.empty());
        when(mapper.toEntity(any())).thenReturn(new MenuEntity());
        when(repository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.createMenu(req));
    }

    @Test
    void testGetMenuById_Found() {
        when(repository.findById(1L)).thenReturn(Optional.of(new MenuEntity()));
        when(mapper.toResponse(any())).thenReturn(new MenuResponse());
        assertNotNull(service.getMenuById(1L));
    }

    @Test
    void testGetMenuById_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getMenuById(1L));
    }
    
    @Test
    void testUpdateMenu_Success() {
        MenuRequest req = new MenuRequest(); req.setLabel("l"); req.setCode("M1"); req.setParentId(null);
        MenuEntity ent = new MenuEntity(); ent.setLabel("x"); ent.setCode("M1");
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(repository.save(any())).thenReturn(ent);
        when(mapper.toResponse(any())).thenReturn(new MenuResponse());
        assertNotNull(service.updateMenu(1L, req));
    }
    
    @Test
    void testUpdateMenu_CodeExists() {
        MenuRequest req = new MenuRequest(); req.setLabel("l"); req.setCode("M2");
        MenuEntity ent = new MenuEntity(); ent.setLabel("x"); ent.setCode("M1");
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(repository.findByCode("M2")).thenReturn(Optional.of(new MenuEntity()));
        assertThrows(BusinessException.class, () -> service.updateMenu(1L, req));
    }
    
    @Test
    void testUpdateMenu_ParentSuccess() {
        MenuRequest req = new MenuRequest(); req.setLabel("l"); req.setCode("M1"); req.setParentId(2L);
        MenuEntity ent = new MenuEntity(); ent.setLabel("x"); ent.setCode("M1");
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(repository.findById(2L)).thenReturn(Optional.of(new MenuEntity()));
        when(repository.save(any())).thenReturn(ent);
        when(mapper.toResponse(any())).thenReturn(new MenuResponse());
        assertNotNull(service.updateMenu(1L, req));
    }
    
    @Test
    void testUpdateMenu_SameParent() {
        MenuRequest req = new MenuRequest(); req.setCode("M1"); req.setParentId(1L);
        MenuEntity ent = new MenuEntity(); ent.setCode("M1");
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        assertThrows(BusinessException.class, () -> service.updateMenu(1L, req));
    }

    @Test
    void testUpdateMenu_ParentNotFound() {
        MenuRequest req = new MenuRequest(); req.setCode("M1"); req.setParentId(2L);
        MenuEntity ent = new MenuEntity(); ent.setCode("M1");
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(repository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateMenu(1L, req));
    }

    @Test
    void testDeleteMenu_Found() {
        MenuEntity ent = new MenuEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        service.deleteMenu(1L);
        verify(repository).delete(ent);
    }
    
    @Test
    void testDeleteMenu_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteMenu(1L));
    }
    
    @Test
    void testGetMenuTree() {
        when(repository.findByParentIsNullOrderByOrderIndexAsc()).thenReturn(List.of(new MenuEntity()));
        when(mapper.toResponse(any())).thenReturn(new MenuResponse());
        assertFalse(service.getMenuTree().isEmpty());
    }
}
