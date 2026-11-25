package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.entity.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

final class PortalContentVisibilityHelper {

    private PortalContentVisibilityHelper() {}

    static boolean isPublishedAndActive(PortalContentEntity c, LocalDateTime now) {
        if (c.getStatus() != PortalContentStatus.PUBLISHED) return false;
        if (c.getPublishFrom() != null && c.getPublishFrom().isAfter(now)) return false;
        return c.getPublishTo() == null || c.getPublishTo().isAfter(now);
    }

    static boolean isEventNotPast(PortalContentEntity c, LocalDateTime now) {
        LocalDateTime cutoff = c.getEventEnd() != null ? c.getEventEnd() : c.getEventStart();
        return cutoff == null || !cutoff.isBefore(now);
    }

    static boolean matchesAudience(PortalContentEntity c, UserEntity user) {
        boolean hasBUs = !c.getBusinessUnits().isEmpty();
        boolean hasRoles = !c.getRoles().isEmpty();
        if (!hasBUs && !hasRoles) return true;

        if (hasBUs && !Boolean.TRUE.equals(user.getHasAllBusinessUnits())) {
            Set<Long> buIds = user.getBusinessUnits().stream()
                    .filter(bu -> Boolean.TRUE.equals(bu.getEnabled()))
                    .map(BusinessUnitEntity::getId)
                    .collect(Collectors.toSet());
            if (c.getBusinessUnits().stream().noneMatch(bu -> buIds.contains(bu.getId()))) {
                return false;
            }
        }
        if (hasRoles) {
            Set<Long> roleIds = user.getRoles().stream()
                    .map(RoleEntity::getId)
                    .collect(Collectors.toSet());
            if (c.getRoles().stream().noneMatch(r -> roleIds.contains(r.getId()))) {
                return false;
            }
        }
        return true;
    }
}
