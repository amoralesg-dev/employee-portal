package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.response.PortalMediaResponse;
import com.rassini.employeeportal.entity.PortalMediaEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface PortalMediaService {
    PortalMediaResponse upload(MultipartFile file, Long userId);
    MediaResource load(Long mediaId);

    record MediaResource(PortalMediaEntity metadata, Resource resource) {}
}
