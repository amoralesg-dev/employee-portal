package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.BusinessUnitRequest;
import com.rassini.employeeportal.dto.response.BusinessUnitResponse;
import com.rassini.employeeportal.entity.BusinessUnitEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.repository.BusinessUnitRepository;
import com.rassini.employeeportal.service.BusinessUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessUnitServiceImpl implements BusinessUnitService {

    private final BusinessUnitRepository businessUnitRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BusinessUnitResponse> getAllBusinessUnits() {
        return businessUnitRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessUnitResponse> getBusinessUnitTree() {
        return businessUnitRepository.findByParentIsNull().stream()
                .map(this::mapToResponseWithChildren)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessUnitResponse getBusinessUnitById(Long id) {
        BusinessUnitEntity entity = businessUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", id));
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public BusinessUnitResponse createBusinessUnit(BusinessUnitRequest request) {
        if (businessUnitRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException("Business Unit with code " + request.getCode() + " already exists");
        }

        BusinessUnitEntity entity = BusinessUnitEntity.builder()
                .code(request.getCode())
                .name(request.getName())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();

        if (request.getParentId() != null) {
            BusinessUnitEntity parent = businessUnitRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit (Parent)", "id", request.getParentId()));
            entity.setParent(parent);
        }

        entity = businessUnitRepository.save(entity);
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public BusinessUnitResponse updateBusinessUnit(Long id, BusinessUnitRequest request) {
        BusinessUnitEntity entity = businessUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", id));

        if (!entity.getCode().equals(request.getCode()) && businessUnitRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException("Business Unit with code " + request.getCode() + " already exists");
        }

        entity.setCode(request.getCode());
        entity.setName(request.getName());
        
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessException("Business Unit cannot be its own parent");
            }
            BusinessUnitEntity parent = businessUnitRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit (Parent)", "id", request.getParentId()));
            entity.setParent(parent);
        } else {
            entity.setParent(null);
        }

        entity = businessUnitRepository.save(entity);
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public void deleteBusinessUnit(Long id) {
        BusinessUnitEntity entity = businessUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", id));
        if (!entity.getChildren().isEmpty()) {
            throw new BusinessException("Cannot delete Business Unit with children");
        }
        businessUnitRepository.delete(entity);
    }

    private BusinessUnitResponse mapToResponse(BusinessUnitEntity entity) {
        return BusinessUnitResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .enabled(entity.getEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private BusinessUnitResponse mapToResponseWithChildren(BusinessUnitEntity entity) {
        BusinessUnitResponse response = mapToResponse(entity);
        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
            response.setChildren(entity.getChildren().stream()
                    .map(this::mapToResponseWithChildren)
                    .collect(Collectors.toList()));
        }
        return response;
    }
}
