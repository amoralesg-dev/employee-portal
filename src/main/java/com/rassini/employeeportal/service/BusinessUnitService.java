package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.request.BusinessUnitRequest;
import com.rassini.employeeportal.dto.response.BusinessUnitResponse;

import java.util.List;

public interface BusinessUnitService {
    List<BusinessUnitResponse> getAllBusinessUnits();
    List<BusinessUnitResponse> getBusinessUnitTree();
    BusinessUnitResponse getBusinessUnitById(Long id);
    BusinessUnitResponse createBusinessUnit(BusinessUnitRequest request);
    BusinessUnitResponse updateBusinessUnit(Long id, BusinessUnitRequest request);
    void deleteBusinessUnit(Long id);
    BusinessUnitResponse updateStatus(Long id, boolean enabled);
    List<com.rassini.employeeportal.dto.response.UserResponse> getBusinessUnitUsers(Long id);
    void replaceBusinessUnitUsers(Long id, java.util.Set<Long> userIds);
}
