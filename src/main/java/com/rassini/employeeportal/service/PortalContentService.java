package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.request.PortalContentRequest;
import com.rassini.employeeportal.dto.response.AudienceOptionsResponse;
import com.rassini.employeeportal.dto.response.PortalContentResponse;
import com.rassini.employeeportal.entity.PortalContentCategory;
import com.rassini.employeeportal.entity.PortalContentStatus;
import com.rassini.employeeportal.entity.PortalContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PortalContentService {
    Page<PortalContentResponse> searchAdmin(PortalContentType type, PortalContentStatus status, PortalContentCategory category, String q, Pageable pageable);
    PortalContentResponse getById(Long id);
    AudienceOptionsResponse getAudienceOptions();
    PortalContentResponse create(PortalContentRequest request, Long userId);
    PortalContentResponse update(Long id, PortalContentRequest request, Long userId);
    PortalContentResponse updateStatus(Long id, PortalContentStatus targetStatus, Long userId);
    void delete(Long id);
}
