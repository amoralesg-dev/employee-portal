package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.dto.response.UserAccessContextResponse;
import com.rassini.employeeportal.entity.MenuEntity;
import com.rassini.employeeportal.entity.PermissionEntity;
import com.rassini.employeeportal.entity.RoleEntity;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccessContextServiceImplTest {
    @InjectMocks private AccessContextServiceImpl service;
    @Mock private UserRepository userRepository;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void testGetAccessContext_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getAccessContext(1L));
    }

    @Test
    void testGetAccessContext_EmptyRoles() {
        UserEntity user = new UserEntity(); user.setId(1L); user.setUsername("u");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserAccessContextResponse res = service.getAccessContext(1L);
        assertTrue(res.getRoles().isEmpty());
        assertTrue(res.getPermissions().isEmpty());
        assertTrue(res.getMenus().isEmpty());
    }

    @Test
    void testGetAccessContext_FullTree() {
        // Build Parent Menu
        MenuEntity parentMenu = new MenuEntity(); 
        parentMenu.setId(10L); parentMenu.setCode("M10"); parentMenu.setOrderIndex(2);

        // Build Child Menu
        MenuEntity childMenu = new MenuEntity(); 
        childMenu.setId(20L); childMenu.setCode("M20"); childMenu.setOrderIndex(1);
        childMenu.setParent(parentMenu);

        // Build sibling to test orderIndex
        MenuEntity childMenu2 = new MenuEntity(); 
        childMenu2.setId(21L); childMenu2.setCode("M21"); childMenu2.setOrderIndex(null);
        childMenu2.setParent(parentMenu);

        // Build second root
        MenuEntity root2 = new MenuEntity(); 
        root2.setId(30L); root2.setCode("M30"); root2.setOrderIndex(1);

        // Permission 1 has childMenu and root2
        PermissionEntity p1 = new PermissionEntity(); p1.setId(100L); p1.setCode("P1");
        p1.setMenus(Set.of(childMenu, root2));

        // Permission 2 has childMenu2
        PermissionEntity p2 = new PermissionEntity(); p2.setId(200L); p2.setCode("P2");
        p2.setMenus(Set.of(childMenu2));

        // Roles
        RoleEntity r1 = new RoleEntity(); r1.setId(1000L); r1.setCode("R1"); r1.setPermissions(Set.of(p1));
        RoleEntity r2 = new RoleEntity(); r2.setId(2000L); r2.setCode("R2"); r2.setPermissions(Set.of(p2));

        UserEntity user = new UserEntity(); user.setId(1L); user.setUsername("u");
        user.setRoles(Set.of(r1, r2));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserAccessContextResponse res = service.getAccessContext(1L);
        
        assertEquals(2, res.getRoles().size());
        assertEquals(2, res.getPermissions().size());
        
        // Roots should be M30 and M10
        assertEquals(2, res.getMenus().size());
        assertEquals("M30", res.getMenus().get(0).getCode()); // orderIndex 1
        assertEquals("M10", res.getMenus().get(1).getCode()); // orderIndex 2
        
        MenuResponse m10Response = res.getMenus().get(1);
        assertEquals(2, m10Response.getChildren().size());
        assertEquals("M20", m10Response.getChildren().get(0).getCode()); // orderIndex 1
        assertEquals("M21", m10Response.getChildren().get(1).getCode()); // orderIndex null (last)
    }
}
