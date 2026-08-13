package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.dto.request.BusinessUnitRequest;
import com.rassini.employeeportal.dto.response.BusinessUnitResponse;
import com.rassini.employeeportal.service.BusinessUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/business-units")
@RequiredArgsConstructor
public class BusinessUnitController {

    private final BusinessUnitService businessUnitService;

    @GetMapping
    public ResponseEntity<List<BusinessUnitResponse>> getAllBusinessUnits() {
        return ResponseEntity.ok(businessUnitService.getAllBusinessUnits());
    }

    @GetMapping("/tree")
    public ResponseEntity<List<BusinessUnitResponse>> getBusinessUnitTree() {
        return ResponseEntity.ok(businessUnitService.getBusinessUnitTree());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessUnitResponse> getBusinessUnitById(@PathVariable Long id) {
        return ResponseEntity.ok(businessUnitService.getBusinessUnitById(id));
    }

    @PostMapping
    public ResponseEntity<BusinessUnitResponse> createBusinessUnit(@Valid @RequestBody BusinessUnitRequest request) {
        return new ResponseEntity<>(businessUnitService.createBusinessUnit(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessUnitResponse> updateBusinessUnit(@PathVariable Long id, @Valid @RequestBody BusinessUnitRequest request) {
        return ResponseEntity.ok(businessUnitService.updateBusinessUnit(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusinessUnit(@PathVariable Long id) {
        businessUnitService.deleteBusinessUnit(id);
        return ResponseEntity.noContent().build();
    }
}
