package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.ChangePasswordRequest;
import com.rassini.employeeportal.dto.request.UpdateStatusRequest;
import com.rassini.employeeportal.dto.request.UserRequest;
import com.rassini.employeeportal.dto.request.UserUpdateRequest;
import com.rassini.employeeportal.dto.response.RoleResponse;
import com.rassini.employeeportal.dto.response.UserResponse;
import com.rassini.employeeportal.entity.RoleEntity;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.RoleMapper;
import com.rassini.employeeportal.mapper.UserMapper;
import com.rassini.employeeportal.repository.RoleRepository;
import com.rassini.employeeportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {
    @InjectMocks private UserServiceImpl service;
    @Mock private UserRepository repository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserMapper mapper;
    @Mock private RoleMapper roleMapper;
    @Mock private PasswordEncoder encoder;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void testGetUsers() {
        when(repository.findAll()).thenReturn(List.of(new UserEntity()));
        when(mapper.toResponse(any())).thenReturn(new UserResponse());
        assertFalse(service.getUsers().isEmpty());
    }

    @Test
    void testCreateUser_Success() {
        UserRequest req = new UserRequest(); req.setUsername("U1"); req.setEmail("e@e.com");
        when(repository.existsByUsername("U1")).thenReturn(false);
        when(repository.existsByEmail("e@e.com")).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(new UserEntity());
        when(repository.save(any())).thenReturn(new UserEntity());
        when(mapper.toResponse(any())).thenReturn(new UserResponse());
        assertNotNull(service.createUser(req));
    }
    
    @Test
    void testCreateUser_UsernameExists() {
        UserRequest req = new UserRequest(); req.setUsername("U1");
        when(repository.existsByUsername("U1")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.createUser(req));
    }
    
    @Test
    void testCreateUser_EmailExists() {
        UserRequest req = new UserRequest(); req.setUsername("U1"); req.setEmail("e@e.com");
        when(repository.existsByUsername("U1")).thenReturn(false);
        when(repository.existsByEmail("e@e.com")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.createUser(req));
    }

    @Test
    void testGetUserById_Found() {
        when(repository.findById(1L)).thenReturn(Optional.of(new UserEntity()));
        when(mapper.toResponse(any())).thenReturn(new UserResponse());
        assertNotNull(service.getUserById(1L));
    }
    
    @Test
    void testGetUserById_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getUserById(1L));
    }

    @Test
    void testUpdateUser_Success() {
        UserUpdateRequest req = new UserUpdateRequest(); req.setUsername("u1"); req.setEmail("e@e.com");
        UserEntity ent = new UserEntity(); ent.setUsername("u1"); ent.setEmail("e@e.com");
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(repository.save(any())).thenReturn(ent);
        when(mapper.toResponse(any())).thenReturn(new UserResponse());
        assertNotNull(service.updateUser(1L, req));
    }
    
    @Test
    void testUpdateUser_UsernameChangedAndExists() {
        UserUpdateRequest req = new UserUpdateRequest(); req.setUsername("u2");
        UserEntity ent = new UserEntity(); ent.setUsername("u1");
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(repository.existsByUsername("u2")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.updateUser(1L, req));
    }

    @Test
    void testUpdateUser_EmailChangedAndExists() {
        UserUpdateRequest req = new UserUpdateRequest(); req.setEmail("e2@e.com");
        UserEntity ent = new UserEntity(); ent.setEmail("e1@e.com");
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(repository.existsByEmail("e2@e.com")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.updateUser(1L, req));
    }

    @Test
    void testUpdateStatus() {
        UpdateStatusRequest req = new UpdateStatusRequest(); req.setEnabled(true);
        UserEntity ent = new UserEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(ent));
        when(repository.save(any())).thenReturn(ent);
        when(mapper.toResponse(any())).thenReturn(new UserResponse());
        assertNotNull(service.updateStatus(1L, req));
    }

    @Test
    void testChangePassword_Success() {
        ChangePasswordRequest req = new ChangePasswordRequest(); 
        req.setCurrentPassword("a"); req.setNewPassword("b"); req.setConfirmPassword("b");
        UserEntity u = new UserEntity(); u.setPasswordHash("x");
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(encoder.matches("b", "x")).thenReturn(false);
        when(encoder.matches("a", "x")).thenReturn(true);
        when(encoder.encode("b")).thenReturn("y");
        assertNotNull(service.changePassword(1L, req));
    }
    
    @Test
    void testChangePassword_MismatchConfirm() {
        ChangePasswordRequest req = new ChangePasswordRequest(); 
        req.setCurrentPassword("a"); req.setNewPassword("b"); req.setConfirmPassword("c");
        UserEntity u = new UserEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        assertThrows(BusinessException.class, () -> service.changePassword(1L, req));
    }
    
    @Test
    void testChangePassword_SameAsCurrent() {
        ChangePasswordRequest req = new ChangePasswordRequest(); 
        req.setCurrentPassword("a"); req.setNewPassword("b"); req.setConfirmPassword("b");
        UserEntity u = new UserEntity(); u.setPasswordHash("x");
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(encoder.matches("b", "x")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.changePassword(1L, req));
    }
    
    @Test
    void testChangePassword_WrongCurrent() {
        ChangePasswordRequest req = new ChangePasswordRequest(); 
        req.setCurrentPassword("a"); req.setNewPassword("b"); req.setConfirmPassword("b");
        UserEntity u = new UserEntity(); u.setPasswordHash("x");
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(encoder.matches("b", "x")).thenReturn(false);
        when(encoder.matches("a", "x")).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.changePassword(1L, req));
    }

    @Test
    void testGetUserRoles() {
        UserEntity u = new UserEntity(); u.setRoles(Set.of(new RoleEntity()));
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(roleMapper.toResponseShallow(any())).thenReturn(new RoleResponse());
        assertFalse(service.getUserRoles(1L).isEmpty());
    }

    @Test
    void testReplaceUserRoles_Success() {
        UserEntity u = new UserEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(roleRepository.findAllById(Set.of(1L))).thenReturn(List.of(new RoleEntity()));
        when(repository.save(any())).thenReturn(u);
        when(mapper.toResponse(any())).thenReturn(new UserResponse());
        assertNotNull(service.replaceUserRoles(1L, Set.of(1L)));
    }
    
    @Test
    void testReplaceUserRoles_NotFound() {
        UserEntity u = new UserEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(roleRepository.findAllById(Set.of(1L))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.replaceUserRoles(1L, Set.of(1L)));
    }
}
