package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.ApplicationDto;
import com.rassini.employeeportal.entity.ApplicationEntity;
import com.rassini.employeeportal.repository.ApplicationRepository;
import com.rassini.employeeportal.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Override
    @Transactional
    public ApplicationDto create(ApplicationDto dto) {
        ApplicationEntity entity = mapToEntity(dto);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        ApplicationEntity saved = applicationRepository.save(entity);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDto getById(Long id) {
        return applicationRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Application not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDto getByCode(String code) {
        return applicationRepository.findByCode(code)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Application not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationDto> getAll() {
        return applicationRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationDto> getActive() {
        return applicationRepository.findByActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApplicationDto update(Long id, ApplicationDto dto) {
        ApplicationEntity entity = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive());
        entity.setUpdatedAt(LocalDateTime.now());
        ApplicationEntity saved = applicationRepository.save(entity);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        applicationRepository.deleteById(id);
    }

    private ApplicationDto mapToDto(ApplicationEntity entity) {
        if (entity == null) return null;
        return ApplicationDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.getActive())
                .build();
    }

    private ApplicationEntity mapToEntity(ApplicationDto dto) {
        if (dto == null) return null;
        return ApplicationEntity.builder()
                .id(dto.getId())
                .code(dto.getCode())
                .name(dto.getName())
                .description(dto.getDescription())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }
}
