package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.dto.ApplicationDto;
import com.rassini.employeeportal.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public List<ApplicationDto> getAll() {
        return applicationService.getAll();
    }

    @GetMapping("/active")
    public List<ApplicationDto> getActive() {
        return applicationService.getActive();
    }

    @GetMapping("/{id}")
    public ApplicationDto getById(@PathVariable Long id) {
        return applicationService.getById(id);
    }

    @GetMapping("/code/{code}")
    public ApplicationDto getByCode(@PathVariable String code) {
        return applicationService.getByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDto create(@RequestBody ApplicationDto dto) {
        return applicationService.create(dto);
    }

    @PutMapping("/{id}")
    public ApplicationDto update(@PathVariable Long id, @RequestBody ApplicationDto dto) {
        return applicationService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        applicationService.delete(id);
    }
}
