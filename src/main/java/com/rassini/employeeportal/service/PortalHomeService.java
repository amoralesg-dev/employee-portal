package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.response.HomeItemResponse;
import com.rassini.employeeportal.dto.response.HomeResponse;

public interface PortalHomeService {
    HomeResponse getHome(Long userId, String lang);
    HomeItemResponse getHomeItemDetail(Long userId, Long contentId, String lang);
}
