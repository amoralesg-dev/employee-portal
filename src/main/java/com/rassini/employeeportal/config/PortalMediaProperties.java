package com.rassini.employeeportal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "portal.media")
public class PortalMediaProperties {
    private String path = "./data/media";
    private long maxSizeBytes = 5L * 1024L * 1024L;
}
