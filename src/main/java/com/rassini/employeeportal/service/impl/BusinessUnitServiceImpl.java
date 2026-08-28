package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.BusinessUnitRequest;
import com.rassini.employeeportal.dto.response.BusinessUnitResponse;
import com.rassini.employeeportal.entity.BusinessUnitEntity;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.BusinessUnitMapper;
import com.rassini.employeeportal.mapper.UserMapper;
import com.rassini.employeeportal.repository.BusinessUnitRepository;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.BusinessUnitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class BusinessUnitServiceImpl implements BusinessUnitService {

    private final BusinessUnitRepository businessUnitRepository;
    private final BusinessUnitMapper businessUnitMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public BusinessUnitServiceImpl(BusinessUnitRepository businessUnitRepository,
                                   BusinessUnitMapper businessUnitMapper,
                                   UserRepository userRepository,
                                   UserMapper userMapper) {
        this.businessUnitRepository = businessUnitRepository;
        this.businessUnitMapper = businessUnitMapper;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessUnitResponse> getAllBusinessUnits() {
        return businessUnitMapper.toResponseList(businessUnitRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessUnitResponse> getBusinessUnitTree() {
        return businessUnitRepository.findByParentIsNull().stream()
                .map(businessUnitMapper::toResponseWithChildren)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessUnitResponse getBusinessUnitById(Long id) {
        BusinessUnitEntity entity = businessUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", id));
        return businessUnitMapper.toResponse(entity);
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
        return businessUnitMapper.toResponse(entity);
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
        return businessUnitMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteBusinessUnit(Long id) {
        BusinessUnitEntity entity = businessUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", id));
        if (!entity.getChildren().isEmpty()) {
            throw new BusinessException("Cannot delete Business Unit with children");
        }
        if (entity.getUsers() != null && !entity.getUsers().isEmpty()) {
            throw new BusinessException("Cannot delete Business Unit with assigned users");
        }
        businessUnitRepository.delete(entity);
        log.info("BU_AUDIT | action=DELETED | id={} | code={}", id, entity.getCode());
    }

    @Override
    @Transactional
    public BusinessUnitResponse updateStatus(Long id, boolean enabled) {
        BusinessUnitEntity entity = businessUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", id));
        
        entity.setEnabled(enabled);
        if (!enabled) {
            // Eliminar si hay que hacer algo al deshabilitar (actualmente nada obligatorio en la entidad de usuario)
        }
        
        BusinessUnitEntity saved = businessUnitRepository.save(entity);
        log.info("BU_AUDIT | action=STATUS_UPDATED | id={} | code={} | enabled={}", id, entity.getCode(), enabled);
        return businessUnitMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.rassini.employeeportal.dto.response.UserResponse> getBusinessUnitUsers(Long id) {
        BusinessUnitEntity entity = businessUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", id));
        
        return entity.getUsers().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void replaceBusinessUnitUsers(Long id, Set<Long> userIds) {
        BusinessUnitEntity entity = businessUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", id));
        
        if (!Boolean.TRUE.equals(entity.getEnabled()) && !userIds.isEmpty()) {
            throw new BusinessException("Cannot assign users to a disabled Business Unit");
        }

        List<UserEntity> users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) {
            throw new BusinessException("One or more user IDs do not exist");
        }

        // Remover esta BU de los usuarios que ya no están asignados
        if (entity.getUsers() != null) {
            for (UserEntity oldUser : new HashSet<>(entity.getUsers())) {
                if (!userIds.contains(oldUser.getId())) {
                    oldUser.getBusinessUnits().remove(entity);
                    userRepository.save(oldUser);
                }
            }
        }

        // Agregar esta BU a los nuevos usuarios asignados
        for (UserEntity newUser : users) {
            if (Boolean.TRUE.equals(newUser.getHasAllBusinessUnits())) {
                throw new BusinessException("Cannot assign Business Unit to a global access user: " + newUser.getUsername());
            }
            newUser.getBusinessUnits().add(entity);
            userRepository.save(newUser);
        }

        log.info("BU_AUDIT | action=USERS_REPLACED | id={} | code={} | userCount={}", id, entity.getCode(), userIds.size());
    }


}
