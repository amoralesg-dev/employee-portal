package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.entity.*;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.repository.PortalContentRepository;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.PortalEventCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalEventCalendarServiceImpl implements PortalEventCalendarService {

    public static final String TIMEZONE_ID = "America/Mexico_City";
    private static final ZoneId MEXICO_ZONE = ZoneId.of(TIMEZONE_ID);
    private static final DateTimeFormatter LOCAL_ICS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter UTC_ICS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final String LANG_ES = "es-MX";
    private static final String LANG_EN = "en-US";

    private final PortalContentRepository contentRepository;
    private final UserRepository userRepository;

    @Override
    public CalendarDownload generateIcs(Long userId, Long contentId, String lang) {
        String requestedLang = normalizeLanguage(lang);
        LocalDateTime now = LocalDateTime.now(MEXICO_ZONE);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", userId));

        PortalContentEntity entity = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", contentId));

        if (entity.getContentType() != PortalContentType.EVENT
                || entity.getEventStart() == null
                || !PortalContentVisibilityHelper.isPublishedAndActive(entity, now)
                || !PortalContentVisibilityHelper.matchesAudience(entity, user)
                || !PortalContentVisibilityHelper.isEventNotPast(entity, now)) {
            throw new ResourceNotFoundException("Evento", "id", contentId);
        }

        PortalContentTranslationEntity translation = resolveTranslation(entity, requestedLang)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", contentId));

        LocalDateTime start = entity.getEventStart();
        LocalDateTime end = entity.getEventEnd() != null ? entity.getEventEnd() : start.plusHours(1);
        String dtStamp = ZonedDateTime.now(ZoneOffset.UTC).format(UTC_ICS_FORMAT);

        StringBuilder description = new StringBuilder();
        if (translation.getSummary() != null && !translation.getSummary().isBlank()) {
            description.append(translation.getSummary().trim());
        }
        if (entity.getTeamsUrl() != null && !entity.getTeamsUrl().isBlank()) {
            if (!description.isEmpty()) description.append("\n\n");
            description.append("Microsoft Teams: ").append(entity.getTeamsUrl().trim());
        }
        if (entity.getLinkUrl() != null && !entity.getLinkUrl().isBlank()
                && (entity.getLinkUrl().startsWith("http://") || entity.getLinkUrl().startsWith("https://"))) {
            if (!description.isEmpty()) description.append("\n\n");
            description.append(entity.getLinkUrl().trim());
        }

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//Rassini//Employee Portal//ES\r\n");
        ics.append("CALSCALE:GREGORIAN\r\n");
        ics.append("METHOD:PUBLISH\r\n");
        ics.append("BEGIN:VTIMEZONE\r\n");
        ics.append("TZID:").append(TIMEZONE_ID).append("\r\n");
        ics.append("BEGIN:STANDARD\r\n");
        ics.append("DTSTART:19700101T000000\r\n");
        ics.append("TZOFFSETFROM:-0600\r\n");
        ics.append("TZOFFSETTO:-0600\r\n");
        ics.append("TZNAME:CST\r\n");
        ics.append("END:STANDARD\r\n");
        ics.append("END:VTIMEZONE\r\n");
        ics.append("BEGIN:VEVENT\r\n");
        ics.append("UID:portal-event-").append(entity.getId()).append("@rassini.com\r\n");
        ics.append("DTSTAMP:").append(dtStamp).append("\r\n");
        ics.append("DTSTART;TZID=").append(TIMEZONE_ID).append(":").append(start.format(LOCAL_ICS_FORMAT)).append("\r\n");
        ics.append("DTEND;TZID=").append(TIMEZONE_ID).append(":").append(end.format(LOCAL_ICS_FORMAT)).append("\r\n");
        ics.append("SUMMARY:").append(escapeIcs(translation.getTitle())).append("\r\n");
        if (!description.isEmpty()) {
            ics.append("DESCRIPTION:").append(escapeIcs(description.toString())).append("\r\n");
        }
        if (entity.getEventLocation() != null && !entity.getEventLocation().isBlank()) {
            ics.append("LOCATION:").append(escapeIcs(entity.getEventLocation().trim())).append("\r\n");
        }
        String eventUrl = entity.getTeamsUrl() != null && !entity.getTeamsUrl().isBlank()
                ? entity.getTeamsUrl().trim()
                : (entity.getLinkUrl() != null && (entity.getLinkUrl().startsWith("http://") || entity.getLinkUrl().startsWith("https://"))
                    ? entity.getLinkUrl().trim() : null);
        if (eventUrl != null) {
            ics.append("URL:").append(eventUrl).append("\r\n");
        }
        ics.append("END:VEVENT\r\n");
        ics.append("END:VCALENDAR\r\n");

        String safeFilename = buildSafeFilename(translation.getTitle(), entity.getId());
        return new CalendarDownload(safeFilename, ics.toString());
    }

    private String buildSafeFilename(String title, Long id) {
        String normalized = Normalizer.normalize(title == null ? "" : title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isEmpty()) {
            normalized = "evento";
        }
        if (normalized.length() > 50) {
            normalized = normalized.substring(0, 50).replaceAll("-+$", "");
        }
        return normalized + "-" + id + ".ics";
    }

    private String escapeIcs(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }

    private Optional<PortalContentTranslationEntity> resolveTranslation(PortalContentEntity c, String requestedLang) {
        if (c.getTranslations() == null || c.getTranslations().isEmpty()) return Optional.empty();
        Optional<PortalContentTranslationEntity> exact = c.getTranslations().stream()
                .filter(t -> requestedLang.equals(t.getLanguage()) && t.getTitle() != null && !t.getTitle().isBlank())
                .findFirst();
        if (exact.isPresent()) return exact;
        if (!LANG_ES.equals(requestedLang)) {
            return c.getTranslations().stream()
                    .filter(t -> LANG_ES.equals(t.getLanguage()) && t.getTitle() != null && !t.getTitle().isBlank())
                    .findFirst();
        }
        return Optional.empty();
    }

    private String normalizeLanguage(String lang) {
        if (lang == null || lang.isBlank()) return LANG_ES;
        String lower = lang.trim().toLowerCase(Locale.ROOT);
        return (lower.equals("en") || lower.equals("en-us")) ? LANG_EN : LANG_ES;
    }
}
