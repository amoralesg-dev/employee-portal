package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.ApplicationDto;
import java.util.List;

public interface ApplicationService {
    ApplicationDto create(ApplicationDto dto);
    ApplicationDto getById(Long id);
    ApplicationDto getByCode(String code);
    List<ApplicationDto> getAll();
    List<ApplicationDto> getActive();
    ApplicationDto update(Long id, ApplicationDto dto);
    void delete(Long id);
}
