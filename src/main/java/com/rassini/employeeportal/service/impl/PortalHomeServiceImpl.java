package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.config.PortalHomeProperties;
import com.rassini.employeeportal.dto.response.HomeItemResponse;
import com.rassini.employeeportal.dto.response.HomeResponse;
import com.rassini.employeeportal.entity.*;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.PortalContentMapper;
import com.rassini.employeeportal.repository.PortalContentRepository;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.PortalHomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalHomeServiceImpl implements PortalHomeService {

    private static final String LANG_ES = "es-MX";
    private static final String LANG_EN = "en-US";

    private final PortalContentRepository contentRepository;
    private final UserRepository userRepository;
    private final PortalContentMapper mapper;
    private final PortalHomeProperties limits;

    @Override
    public HomeResponse getHome(Long userId, String lang) {
        String requestedLang = normalizeLanguage(lang);
        LocalDateTime now = LocalDateTime.now();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", userId));

        UserAudienceContext audience = buildAudienceContext(user);
        List<PortalContentEntity> candidates = contentRepository.findActivePublished(PortalContentStatus.PUBLISHED, now);

        Comparator<PortalContentEntity> defaultOrder = Comparator
                .comparing(PortalContentEntity::getPriority, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PortalContentEntity::getPublishFrom, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PortalContentEntity::getId, Comparator.nullsLast(Comparator.reverseOrder()));

        Comparator<PortalContentEntity> eventOrder = Comparator
                .comparing(PortalContentEntity::getEventStart, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(defaultOrder);

        List<PortalContentEntity> visible = candidates.stream()
                .filter(c -> matchesAudience(c, audience))
                .filter(c -> isEventNotPast(c, now))
                .toList();

        List<HomeItemResponse> heroes = mapVisible(visible, PortalContentType.HERO_NEWS, defaultOrder, requestedLang, Integer.MAX_VALUE);
        HomeItemResponse hero = heroes.isEmpty() ? null : heroes.get(0);

        List<HomeItemResponse> secondaryNews = new ArrayList<>();
        if (heroes.size() > 1) {
            secondaryNews.addAll(heroes.subList(1, heroes.size()));
        }
        secondaryNews.addAll(mapVisible(visible, PortalContentType.NEWS, defaultOrder, requestedLang, Integer.MAX_VALUE));
        if (secondaryNews.size() > limits.getSecondaryNews()) {
            secondaryNews = secondaryNews.subList(0, limits.getSecondaryNews());
        }

        return HomeResponse.builder()
                .requestedLanguage(requestedLang)
                .generatedAt(now)
                .hero(hero)
                .secondaryNews(secondaryNews)
                .campaigns(mapVisible(visible, PortalContentType.CAMPAIGN, defaultOrder, requestedLang, limits.getCampaigns()))
                .weeklyInfo(mapVisible(visible, PortalContentType.WEEKLY_INFO, defaultOrder, requestedLang, limits.getWeeklyInfo()))
                .events(mapVisible(visible, PortalContentType.EVENT, eventOrder, requestedLang, limits.getEvents()))
                .quickAccess(mapVisible(visible, PortalContentType.QUICK_ACCESS, defaultOrder, requestedLang, limits.getQuickAccess()))
                .notices(mapVisible(visible, PortalContentType.NOTICE, defaultOrder, requestedLang, limits.getNotices()))
                .build();
    }

    @Override
    public HomeItemResponse getHomeItemDetail(Long userId, Long contentId, String lang) {
        String requestedLang = normalizeLanguage(lang);
        LocalDateTime now = LocalDateTime.now();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", userId));

        PortalContentEntity entity = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido", "id", contentId));

        if (!isPublishedAndActive(entity, now)
                || !matchesAudience(entity, buildAudienceContext(user))
                || !isEventNotPast(entity, now)) {
            throw new ResourceNotFoundException("Contenido", "id", contentId);
        }

        PortalContentTranslationEntity translation = resolveTranslation(entity, requestedLang)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido", "id", contentId));

        return mapper.toHomeItem(entity, translation, true);
    }

    private List<HomeItemResponse> mapVisible(
            List<PortalContentEntity> visible,
            PortalContentType type,
            Comparator<PortalContentEntity> comparator,
            String requestedLang,
            int limit) {
        return visible.stream()
                .filter(c -> c.getContentType() == type)
                .sorted(comparator)
                .map(c -> resolveTranslation(c, requestedLang).map(t -> mapper.toHomeItem(c, t, false)))
                .flatMap(Optional::stream)
                .limit(limit)
                .toList();
    }

    private boolean isPublishedAndActive(PortalContentEntity c, LocalDateTime now) {
        if (c.getStatus() != PortalContentStatus.PUBLISHED) {
            return false;
        }
        if (c.getPublishFrom() != null && c.getPublishFrom().isAfter(now)) {
            return false;
        }
        return c.getPublishTo() == null || c.getPublishTo().isAfter(now);
    }

    private boolean isEventNotPast(PortalContentEntity c, LocalDateTime now) {
        if (c.getContentType() != PortalContentType.EVENT) {
            return true;
        }
        LocalDateTime cutoff = c.getEventEnd() != null ? c.getEventEnd() : c.getEventStart();
        return cutoff == null || !cutoff.isBefore(now);
    }

    private boolean matchesAudience(PortalContentEntity c, UserAudienceContext ctx) {
        boolean hasBUs = c.getBusinessUnits() != null && !c.getBusinessUnits().isEmpty();
        boolean hasRoles = c.getRoles() != null && !c.getRoles().isEmpty();

        if (!hasBUs && !hasRoles) {
            return true;
        }
        if (hasBUs) {
            boolean buMatch = ctx.hasAllBUs() || c.getBusinessUnits().stream()
                    .anyMatch(bu -> ctx.buIds().contains(bu.getId()));
            if (!buMatch) {
                return false;
            }
        }
        if (hasRoles) {
            boolean roleMatch = c.getRoles().stream()
                    .anyMatch(role -> ctx.roleIds().contains(role.getId()));
            if (!roleMatch) {
                return false;
            }
        }
        return true;
    }

    private Optional<PortalContentTranslationEntity> resolveTranslation(PortalContentEntity c, String requestedLang) {
        if (c.getTranslations() == null || c.getTranslations().isEmpty()) {
            return Optional.empty();
        }
        Optional<PortalContentTranslationEntity> exact = c.getTranslations().stream()
                .filter(t -> requestedLang.equals(t.getLanguage()) && t.getTitle() != null && !t.getTitle().isBlank())
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        if (!LANG_ES.equals(requestedLang)) {
            return c.getTranslations().stream()
                    .filter(t -> LANG_ES.equals(t.getLanguage()) && t.getTitle() != null && !t.getTitle().isBlank())
                    .findFirst();
        }
        return Optional.empty();
    }

    private String normalizeLanguage(String lang) {
        if (lang == null || lang.isBlank()) {
            return LANG_ES;
        }
        String lower = lang.trim().toLowerCase(Locale.ROOT);
        if (lower.equals("en") || lower.equals("en-us")) {
            return LANG_EN;
        }
        return LANG_ES;
    }

    private UserAudienceContext buildAudienceContext(UserEntity user) {
        boolean hasAll = Boolean.TRUE.equals(user.getHasAllBusinessUnits());
        Set<Long> buIds = user.getBusinessUnits() == null ? Set.of() : user.getBusinessUnits().stream()
                .filter(bu -> Boolean.TRUE.equals(bu.getEnabled()))
                .map(BusinessUnitEntity::getId)
                .collect(Collectors.toSet());
        Set<Long> roleIds = user.getRoles() == null ? Set.of() : user.getRoles().stream()
                .map(RoleEntity::getId)
                .collect(Collectors.toSet());
        return new UserAudienceContext(hasAll, buIds, roleIds);
    }

    private record UserAudienceContext(boolean hasAllBUs, Set<Long> buIds, Set<Long> roleIds) {}
}
