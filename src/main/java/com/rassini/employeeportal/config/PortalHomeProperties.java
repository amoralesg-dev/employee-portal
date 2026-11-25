package com.rassini.employeeportal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "portal.home.limits")
public class PortalHomeProperties {
    private int secondaryNews = 4;
    private int campaigns = 3;
    private int weeklyInfo = 5;
    private int events = 5;
    private int quickAccess = 8;
    private int notices = 5;
}
