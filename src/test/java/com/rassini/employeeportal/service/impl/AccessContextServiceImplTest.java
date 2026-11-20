package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.dto.response.UserAccessContextResponse;
import com.rassini.employeeportal.entity.ApplicationEntity;
import com.rassini.employeeportal.entity.MenuEntity;
import com.rassini.employeeportal.entity.PermissionEntity;
import com.rassini.employeeportal.entity.RoleEntity;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.BusinessUnitMapper;
import com.rassini.employeeportal.repository.BusinessUnitRepository;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.MenuUrlResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccessContextServiceImplTest {
    @InjectMocks private AccessContextServiceImpl service;
    @Mock private UserRepository userRepository;
    @Mock private BusinessUnitRepository businessUnitRepository;
    @Mock private BusinessUnitMapper businessUnitMapper;
    @Mock private MenuUrlResolverService menuUrlResolverService;
    @Mock private Environment environment;

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

    @Test
    void testGetAccessContext_ApplicationCodeAndResolvedUrlResolution() {
        ApplicationEntity iamApp = ApplicationEntity.builder()
                .id(1L)
                .code("EMPLOYEE_PORTAL")
                .name("Portal de Empleados")
                .build();

        ApplicationEntity pagosApp = ApplicationEntity.builder()
                .id(3L)
                .code("MS_PAGOS")
                .name("Portal Pagos")
                .build();

        // 1. applicationCode presente + route con '/' + URL base configurada (con barra final)
        MenuEntity menuPermisos = new MenuEntity();
        menuPermisos.setId(101L);
        menuPermisos.setCode("PERMISSIONS");
        menuPermisos.setRoute("/permisos");
        menuPermisos.setOrderIndex(1);
        menuPermisos.setApplication(iamApp);

        // 2. applicationCode presente + route sin '/' inicial + URL base configurada
        MenuEntity menuUsuarios = new MenuEntity();
        menuUsuarios.setId(102L);
        menuUsuarios.setCode("USERS");
        menuUsuarios.setRoute("usuarios");
        menuUsuarios.setOrderIndex(2);
        menuUsuarios.setApplication(iamApp);

        // 3. applicationCode presente + route ausente (null) y blank
        MenuEntity menuGrupoIam = new MenuEntity();
        menuGrupoIam.setId(103L);
        menuGrupoIam.setCode("IAM_GROUP");
        menuGrupoIam.setRoute(null);
        menuGrupoIam.setOrderIndex(3);
        menuGrupoIam.setApplication(iamApp);

        MenuEntity menuBlankRoute = new MenuEntity();
        menuBlankRoute.setId(104L);
        menuBlankRoute.setCode("IAM_BLANK_ROUTE");
        menuBlankRoute.setRoute("   ");
        menuBlankRoute.setOrderIndex(4);
        menuBlankRoute.setApplication(iamApp);

        // 4. resolvedUrl existente devuelto por MenuUrlResolverService (no debe sobrescribirse)
        MenuEntity menuExternalResolved = new MenuEntity();
        menuExternalResolved.setId(105L);
        menuExternalResolved.setCode("PAGOS_PENDIENTES");
        menuExternalResolved.setRoute("/pagos-pendientes");
        menuExternalResolved.setOrderIndex(5);
        menuExternalResolved.setApplication(pagosApp);

        // 5. URL base no configurada (null y blank)
        MenuEntity menuUnconfiguredBase = new MenuEntity();
        menuUnconfiguredBase.setId(106L);
        menuUnconfiguredBase.setCode("PAGOS_HISTORIAL");
        menuUnconfiguredBase.setRoute("/historial");
        menuUnconfiguredBase.setOrderIndex(6);
        menuUnconfiguredBase.setApplication(pagosApp);

        ApplicationEntity demoApp = ApplicationEntity.builder()
                .id(2L)
                .code("PORTAL_DEMO")
                .build();
        MenuEntity menuBlankBase = new MenuEntity();
        menuBlankBase.setId(107L);
        menuBlankBase.setCode("DEMO_ITEM");
        menuBlankBase.setRoute("/demo");
        menuBlankBase.setOrderIndex(7);
        menuBlankBase.setApplication(demoApp);

        // 6. applicationCode ausente (application == null)
        MenuEntity menuNoApp = new MenuEntity();
        menuNoApp.setId(108L);
        menuNoApp.setCode("ORPHAN_MENU");
        menuNoApp.setRoute("/orphan");
        menuNoApp.setOrderIndex(8);
        menuNoApp.setApplication(null);

        PermissionEntity perm = new PermissionEntity();
        perm.setId(500L);
        perm.setCode("PERM_ALL");
        perm.setMenus(Set.of(
                menuPermisos,
                menuUsuarios,
                menuGrupoIam,
                menuBlankRoute,
                menuExternalResolved,
                menuUnconfiguredBase,
                menuBlankBase,
                menuNoApp
        ));

        RoleEntity role = new RoleEntity();
        role.setId(600L);
        role.setCode("ROLE_ADMIN");
        role.setPermissions(Set.of(perm));

        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("admin");
        user.setRoles(Set.of(role));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(menuUrlResolverService.resolveUrl(any(MenuEntity.class), eq(user))).thenReturn(null);
        when(menuUrlResolverService.resolveUrl(eq(menuPermisos), eq(user))).thenReturn("   ");
        when(menuUrlResolverService.resolveUrl(eq(menuExternalResolved), eq(user)))
                .thenReturn("https://rss-cxp-dev.rassini.com/portal-cxp/pagos-pendientes");

        when(environment.getProperty("corporate.navigation.app-base-urls.EMPLOYEE_PORTAL"))
                .thenReturn("https://rss-cxp-dev.rassini.com/employee-portal/");
        when(environment.getProperty("corporate.navigation.app-base-urls.MS_PAGOS"))
                .thenReturn(null);
        when(environment.getProperty("corporate.navigation.app-base-urls.PORTAL_DEMO"))
                .thenReturn("   ");

        UserAccessContextResponse response = service.getAccessContext(1L);
        Map<String, MenuResponse> byCode = response.getMenus().stream()
                .collect(Collectors.toMap(MenuResponse::getCode, m -> m));

        // 1. route con '/' + baseUrl configurada
        MenuResponse permisosRes = byCode.get("PERMISSIONS");
        assertEquals(1L, permisosRes.getApplicationId());
        assertEquals("EMPLOYEE_PORTAL", permisosRes.getApplicationCode());
        assertEquals("https://rss-cxp-dev.rassini.com/employee-portal/permisos", permisosRes.getResolvedUrl());

        // 2. route sin '/' + baseUrl configurada
        MenuResponse usuariosRes = byCode.get("USERS");
        assertEquals("EMPLOYEE_PORTAL", usuariosRes.getApplicationCode());
        assertEquals("https://rss-cxp-dev.rassini.com/employee-portal/usuarios", usuariosRes.getResolvedUrl());

        // 3. route ausente / blank
        MenuResponse grupoRes = byCode.get("IAM_GROUP");
        assertEquals("EMPLOYEE_PORTAL", grupoRes.getApplicationCode());
        assertNull(grupoRes.getResolvedUrl());

        MenuResponse blankRouteRes = byCode.get("IAM_BLANK_ROUTE");
        assertEquals("EMPLOYEE_PORTAL", blankRouteRes.getApplicationCode());
        assertNull(blankRouteRes.getResolvedUrl());

        // 4. resolvedUrl preexistente preservado (no sobrescrito)
        MenuResponse pagosRes = byCode.get("PAGOS_PENDIENTES");
        assertEquals(3L, pagosRes.getApplicationId());
        assertEquals("MS_PAGOS", pagosRes.getApplicationCode());
        assertEquals("https://rss-cxp-dev.rassini.com/portal-cxp/pagos-pendientes", pagosRes.getResolvedUrl());

        // 5. URL base no configurada (null / blank)
        MenuResponse unconfiguredRes = byCode.get("PAGOS_HISTORIAL");
        assertEquals("MS_PAGOS", unconfiguredRes.getApplicationCode());
        assertNull(unconfiguredRes.getResolvedUrl());

        MenuResponse blankBaseRes = byCode.get("DEMO_ITEM");
        assertEquals("PORTAL_DEMO", blankBaseRes.getApplicationCode());
        assertNull(blankBaseRes.getResolvedUrl());

        // 6. applicationCode ausente
        MenuResponse orphanRes = byCode.get("ORPHAN_MENU");
        assertNull(orphanRes.getApplicationId());
        assertNull(orphanRes.getApplicationCode());
        assertNull(orphanRes.getResolvedUrl());
    }
}
