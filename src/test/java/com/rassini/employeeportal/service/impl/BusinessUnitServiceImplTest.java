package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.BusinessUnitRequest;
import com.rassini.employeeportal.dto.response.BusinessUnitResponse;
import com.rassini.employeeportal.entity.BusinessUnitEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.repository.BusinessUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BusinessUnitServiceImplTest {

    @Mock
    private BusinessUnitRepository businessUnitRepository;

    @InjectMocks
    private BusinessUnitServiceImpl businessUnitService;

    private BusinessUnitEntity rootEntity;
    private BusinessUnitEntity childEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        rootEntity = BusinessUnitEntity.builder()
                .id(1L)
                .code("ROOT")
                .name("Root Unit")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .children(new HashSet<>())
                .build();

        childEntity = BusinessUnitEntity.builder()
                .id(2L)
                .code("CHILD")
                .name("Child Unit")
                .enabled(true)
                .parent(rootEntity)
                .createdAt(LocalDateTime.now())
                .children(new HashSet<>())
                .build();

        rootEntity.getChildren().add(childEntity);
    }

    @Test
    void testGetAllBusinessUnits() {
        when(businessUnitRepository.findAll()).thenReturn(List.of(rootEntity, childEntity));
        List<BusinessUnitResponse> responses = businessUnitService.getAllBusinessUnits();
        assertEquals(2, responses.size());
        assertEquals("ROOT", responses.get(0).getCode());
        assertEquals("CHILD", responses.get(1).getCode());
        assertEquals(2, responses.size());
    }

    @Test
    void testGetBusinessUnitTree() {
        when(businessUnitRepository.findByParentIsNull()).thenReturn(List.of(rootEntity));
        List<BusinessUnitResponse> tree = businessUnitService.getBusinessUnitTree();
        assertEquals(1, tree.size());
        assertEquals("ROOT", tree.get(0).getCode());
        assertNotNull(tree.get(0).getChildren());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals("CHILD", tree.get(0).getChildren().get(0).getCode());
    }

    @Test
    void testGetBusinessUnitByIdSuccess() {
        when(businessUnitRepository.findById(1L)).thenReturn(Optional.of(rootEntity));
        BusinessUnitResponse response = businessUnitService.getBusinessUnitById(1L);
        assertNotNull(response);
        assertEquals("ROOT", response.getCode());
    }

    @Test
    void testGetBusinessUnitByIdNotFound() {
        when(businessUnitRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> businessUnitService.getBusinessUnitById(99L));
    }

    @Test
    void testCreateBusinessUnitSuccessWithoutParent() {
        BusinessUnitRequest request = BusinessUnitRequest.builder()
                .code("NEW")
                .name("New Unit")
                .enabled(true)
                .build();

        when(businessUnitRepository.findByCode("NEW")).thenReturn(Optional.empty());
        when(businessUnitRepository.save(any(BusinessUnitEntity.class))).thenAnswer(invocation -> {
            BusinessUnitEntity saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        BusinessUnitResponse response = businessUnitService.createBusinessUnit(request);
        assertNotNull(response);
        assertEquals("NEW", response.getCode());
        assertNull(response.getParentId());
    }

    @Test
    void testCreateBusinessUnitSuccessWithParent() {
        BusinessUnitRequest request = BusinessUnitRequest.builder()
                .code("NEW")
                .name("New Unit")
                .parentId(1L)
                .build();

        when(businessUnitRepository.findByCode("NEW")).thenReturn(Optional.empty());
        when(businessUnitRepository.findById(1L)).thenReturn(Optional.of(rootEntity));
        when(businessUnitRepository.save(any(BusinessUnitEntity.class))).thenAnswer(invocation -> {
            BusinessUnitEntity saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        BusinessUnitResponse response = businessUnitService.createBusinessUnit(request);
        assertNotNull(response);
        assertEquals(1L, response.getParentId());
    }

    @Test
    void testCreateBusinessUnitAlreadyExists() {
        BusinessUnitRequest request = BusinessUnitRequest.builder()
                .code("ROOT")
                .name("Root Unit Copy")
                .build();

        when(businessUnitRepository.findByCode("ROOT")).thenReturn(Optional.of(rootEntity));
        assertThrows(BusinessException.class, () -> businessUnitService.createBusinessUnit(request));
    }

    @Test
    void testUpdateBusinessUnitSuccess() {
        BusinessUnitRequest request = BusinessUnitRequest.builder()
                .code("ROOT_UPDATED")
                .name("Root Unit Updated")
                .parentId(2L)
                .enabled(false)
                .build();

        when(businessUnitRepository.findById(1L)).thenReturn(Optional.of(rootEntity));
        when(businessUnitRepository.findByCode("ROOT_UPDATED")).thenReturn(Optional.empty());
        when(businessUnitRepository.findById(2L)).thenReturn(Optional.of(childEntity));
        when(businessUnitRepository.save(any(BusinessUnitEntity.class))).thenReturn(rootEntity);

        BusinessUnitResponse response = businessUnitService.updateBusinessUnit(1L, request);
        assertNotNull(response);
        assertEquals("ROOT_UPDATED", response.getCode());
        assertFalse(response.getEnabled());
        assertEquals(2L, response.getParentId());
    }

    @Test
    void testUpdateBusinessUnitSelfParentError() {
        BusinessUnitRequest request = BusinessUnitRequest.builder()
                .code("ROOT")
                .name("Root Unit")
                .parentId(1L)
                .build();

        when(businessUnitRepository.findById(1L)).thenReturn(Optional.of(rootEntity));
        assertThrows(BusinessException.class, () -> businessUnitService.updateBusinessUnit(1L, request));
    }

    @Test
    void testDeleteBusinessUnitSuccess() {
        BusinessUnitEntity emptyEntity = BusinessUnitEntity.builder()
                .id(4L)
                .code("EMPTY")
                .name("Empty Unit")
                .children(new HashSet<>())
                .build();

        when(businessUnitRepository.findById(4L)).thenReturn(Optional.of(emptyEntity));
        doNothing().when(businessUnitRepository).delete(emptyEntity);

        assertDoesNotThrow(() -> businessUnitService.deleteBusinessUnit(4L));
        verify(businessUnitRepository, times(1)).delete(emptyEntity);
    }

    @Test
    void testDeleteBusinessUnitWithChildrenError() {
        when(businessUnitRepository.findById(1L)).thenReturn(Optional.of(rootEntity));
        assertThrows(BusinessException.class, () -> businessUnitService.deleteBusinessUnit(1L));
    }
}
