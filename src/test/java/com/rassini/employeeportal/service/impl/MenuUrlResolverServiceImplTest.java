package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.entity.MenuEntity;
import com.rassini.employeeportal.entity.MenuParameterEntity;
import com.rassini.employeeportal.entity.TargetType;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MenuUrlResolverServiceImplTest {

    private MenuUrlResolverServiceImpl resolver;

    @BeforeEach
    void setUp() {
        resolver = new MenuUrlResolverServiceImpl();
    }

    @Test
    void testValidateParameterSafety_ForbiddenTokens_ThrowsException() {
        assertThrows(BusinessException.class, () -> resolver.validateParameterSafety("token", "${JWT}"));
        assertThrows(BusinessException.class, () -> resolver.validateParameterSafety("p1", "${TOKEN}"));
        assertThrows(BusinessException.class, () -> resolver.validateParameterSafety("p2", "${PASSWORD}"));
        assertThrows(BusinessException.class, () -> resolver.validateParameterSafety("p3", "${SECRET}"));
        assertThrows(BusinessException.class, () -> resolver.validateParameterSafety("p4", "${APIKEY}"));
        assertThrows(BusinessException.class, () -> resolver.validateParameterSafety("p5", "${API_KEY}"));
    }

    @Test
    void testValidateParameterSafety_ForbiddenNames_ThrowsException() {
        assertThrows(BusinessException.class, () -> resolver.validateParameterSafety("jwt", "val"));
        assertThrows(BusinessException.class, () -> resolver.validateParameterSafety("token", "val"));
        assertThrows(BusinessException.class, () -> resolver.validateParameterSafety("password", "val"));
        assertThrows(BusinessException.class, () -> resolver.validateParameterSafety("bearer", "val"));
    }

    @Test
    void testValidateParameterSafety_AllowedTokens_Success() {
        assertDoesNotThrow(() -> resolver.validateParameterSafety("bu", "${BUSINESS_UNIT}"));
        assertDoesNotThrow(() -> resolver.validateParameterSafety("bus", "${BUSINESS_UNITS}"));
        assertDoesNotThrow(() -> resolver.validateParameterSafety("lang", "${LANGUAGE}"));
        assertDoesNotThrow(() -> resolver.validateParameterSafety("theme", "${THEME}"));
        assertDoesNotThrow(() -> resolver.validateParameterSafety("user", "${USERNAME}"));
        assertDoesNotThrow(() -> resolver.validateParameterSafety("email", "${EMAIL}"));
        assertDoesNotThrow(() -> resolver.validateParameterSafety("empId", "${EMPLOYEE_ID}"));
    }

    @Test
    void testResolveUrl_InternalMenu_ReturnsNull() {
        MenuEntity menu = MenuEntity.builder()
                .targetType(TargetType.INTERNO)
                .route("/usuarios")
                .build();
        assertNull(resolver.resolveUrl(menu, null));
    }

    @Test
    void testResolveUrl_ExternalMenuWithoutParams() {
        MenuEntity menu = MenuEntity.builder()
                .targetType(TargetType.EXTERNO)
                .externalUrl("https://portalproveedores.rassini.com/home")
                .build();
        assertEquals("https://portalproveedores.rassini.com/home", resolver.resolveUrl(menu, null));
    }

    @Test
    void testResolveUrl_ExternalMenuWithContextVariables() {
        UserEntity user = UserEntity.builder()
                .id(100L)
                .username("amoralesg")
                .email("amoralesg@rassini.com")
                .build();

        MenuParameterEntity p1 = MenuParameterEntity.builder()
                .id(1L)
                .paramName("user")
                .paramValue("${USERNAME}")
                .active(true)
                .build();

        MenuParameterEntity p2 = MenuParameterEntity.builder()
                .id(2L)
                .paramName("lang")
                .paramValue("${LANGUAGE}")
                .active(true)
                .build();

        MenuEntity menu = MenuEntity.builder()
                .targetType(TargetType.EXTERNO)
                .externalUrl("https://portalproveedores.rassini.com/home")
                .parameters(Set.of(p1, p2))
                .build();

        String resolved = resolver.resolveUrl(menu, user);
        assertNotNull(resolved);
        assertTrue(resolved.contains("user=amoralesg"));
        assertTrue(resolved.contains("lang=es"));
    }

    @Test
    void testResolveUrl_BusinessUnits_SingleBU() {
        com.rassini.employeeportal.entity.BusinessUnitEntity bu = com.rassini.employeeportal.entity.BusinessUnitEntity.builder()
                .id(1L)
                .code("1850")
                .name("Brakes")
                .build();

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("test_user")
                .businessUnits(Set.of(bu))
                .build();

        MenuParameterEntity pSingle = MenuParameterEntity.builder()
                .id(1L)
                .paramName("bu")
                .paramValue("${BUSINESS_UNIT}")
                .active(true)
                .build();

        MenuParameterEntity pMulti = MenuParameterEntity.builder()
                .id(2L)
                .paramName("bus")
                .paramValue("${BUSINESS_UNITS}")
                .active(true)
                .build();

        MenuEntity menuSingle = MenuEntity.builder()
                .targetType(TargetType.EXTERNO)
                .externalUrl("https://example.com")
                .parameters(Set.of(pSingle))
                .build();

        MenuEntity menuMulti = MenuEntity.builder()
                .targetType(TargetType.EXTERNO)
                .externalUrl("https://example.com")
                .parameters(Set.of(pMulti))
                .build();

        assertEquals("https://example.com?bu=1850", resolver.resolveUrl(menuSingle, user));
        assertEquals("https://example.com?bus=1850", resolver.resolveUrl(menuMulti, user));
    }

    @Test
    void testResolveUrl_BusinessUnits_MultipleBUs_Sorted() {
        com.rassini.employeeportal.entity.BusinessUnitEntity bu1 = com.rassini.employeeportal.entity.BusinessUnitEntity.builder()
                .id(1L)
                .code("1850")
                .name("Brakes")
                .build();
        com.rassini.employeeportal.entity.BusinessUnitEntity bu2 = com.rassini.employeeportal.entity.BusinessUnitEntity.builder()
                .id(2L)
                .code("0111")
                .name("Corporativo")
                .build();

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("test_user")
                .businessUnits(Set.of(bu1, bu2))
                .build();

        MenuParameterEntity pMulti = MenuParameterEntity.builder()
                .id(1L)
                .paramName("bus")
                .paramValue("${BUSINESS_UNITS}")
                .active(true)
                .build();

        MenuEntity menu = MenuEntity.builder()
                .targetType(TargetType.EXTERNO)
                .externalUrl("https://example.com")
                .parameters(Set.of(pMulti))
                .build();

        // Must be sorted deterministically: "0111,1850"
        assertEquals("https://example.com?bus=0111%2C1850", resolver.resolveUrl(menu, user));
    }

    @Test
    void testResolveUrl_BusinessUnits_NoBUs_ReturnsEmpty() {
        UserEntity userNoBUs = UserEntity.builder()
                .id(1L)
                .username("test_user")
                .businessUnits(Set.of())
                .build();

        MenuParameterEntity pMulti = MenuParameterEntity.builder()
                .id(1L)
                .paramName("bus")
                .paramValue("${BUSINESS_UNITS}")
                .active(true)
                .build();

        MenuEntity menu = MenuEntity.builder()
                .targetType(TargetType.EXTERNO)
                .externalUrl("https://example.com")
                .parameters(Set.of(pMulti))
                .build();

        assertEquals("https://example.com?bus=", resolver.resolveUrl(menu, userNoBUs));
        assertEquals("https://example.com?bus=", resolver.resolveUrl(menu, null));
    }
}
