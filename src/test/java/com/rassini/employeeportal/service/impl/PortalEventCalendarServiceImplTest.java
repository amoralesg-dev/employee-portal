package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.entity.*;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.repository.PortalContentRepository;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.PortalEventCalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortalEventCalendarServiceImplTest {

    @Mock
    private PortalContentRepository contentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PortalEventCalendarServiceImpl service;

    private UserEntity user;
    private PortalContentEntity event;

    @BeforeEach
    void setUp() {
        BusinessUnitEntity bu = BusinessUnitEntity.builder().id(1L).code("CORP").name("Corporativo").enabled(true).build();
        user = UserEntity.builder().id(10L).username("jdoe").email("jdoe@rassini.com").businessUnits(Set.of(bu)).roles(Set.of()).build();

        event = PortalContentEntity.builder()
                .id(42L)
                .contentType(PortalContentType.EVENT)
                .status(PortalContentStatus.PUBLISHED)
                .eventStart(LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0))
                .eventEnd(LocalDateTime.now().plusDays(2).withHour(11).withMinute(30).withSecond(0).withNano(0))
                .eventLocation("Auditorio Corporativo")
                .teamsUrl("https://teams.microsoft.com/l/meetup-join/abc123")
                .linkUrl("https://rassini.sharepoint.com/events/townhall")
                .businessUnits(Set.of())
                .roles(Set.of())
                .build();
        event.setTranslations(Set.of(
                PortalContentTranslationEntity.builder()
                        .portalContent(event)
                        .language("es-MX")
                        .title("Town Hall Rassini 2026")
                        .summary("Sesión trimestral de resultados")
                        .build(),
                PortalContentTranslationEntity.builder()
                        .portalContent(event)
                        .language("en-US")
                        .title("Rassini Town Hall 2026")
                        .summary("Quarterly results session")
                        .build()
        ));
    }

    @Test
    void generateIcs_shouldIncludeTimezoneTitleLocationTeamsAndUid() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(42L)).thenReturn(Optional.of(event));

        PortalEventCalendarService.CalendarDownload dl = service.generateIcs(10L, 42L, "es-MX");

        assertThat(dl.filename()).isEqualTo("town-hall-rassini-2026-42.ics");
        assertThat(dl.content())
                .contains("BEGIN:VCALENDAR")
                .contains("TZID:America/Mexico_City")
                .contains("UID:portal-event-42@rassini.com")
                .contains("DTSTART;TZID=America/Mexico_City:")
                .contains("T100000")
                .contains("DTEND;TZID=America/Mexico_City:")
                .contains("T113000")
                .contains("SUMMARY:Town Hall Rassini 2026")
                .contains("LOCATION:Auditorio Corporativo")
                .contains("URL:https://teams.microsoft.com/l/meetup-join/abc123")
                .contains("Microsoft Teams: https://teams.microsoft.com/l/meetup-join/abc123")
                .contains("END:VCALENDAR");
    }

    @Test
    void generateIcs_shouldUseEnglishTranslationAndFallbackToSpanish() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(42L)).thenReturn(Optional.of(event));

        PortalEventCalendarService.CalendarDownload enDl = service.generateIcs(10L, 42L, "en-US");
        assertThat(enDl.content()).contains("SUMMARY:Rassini Town Hall 2026");

        // Remove en-US to verify fallback
        event.setTranslations(Set.of(
                PortalContentTranslationEntity.builder().portalContent(event).language("es-MX").title("Solo Español").build()
        ));
        PortalEventCalendarService.CalendarDownload fallbackDl = service.generateIcs(10L, 42L, "en-US");
        assertThat(fallbackDl.content()).contains("SUMMARY:Solo Español");
    }

    @Test
    void generateIcs_withExactOneMinuteRange_shouldPreserveExactDtStartAndDtEnd() {
        event.setEventStart(LocalDateTime.of(2026, 11, 27, 10, 59, 0));
        event.setEventEnd(LocalDateTime.of(2026, 11, 27, 11, 0, 0));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(42L)).thenReturn(Optional.of(event));

        PortalEventCalendarService.CalendarDownload dl = service.generateIcs(10L, 42L, "es-MX");
        assertThat(dl.content())
                .contains("DTSTART;TZID=America/Mexico_City:20261127T105900")
                .contains("DTEND;TZID=America/Mexico_City:20261127T110000");
    }

    @Test
    void generateIcs_withoutEventEnd_shouldDefaultToOneHourDuration() {
        event.setEventStart(LocalDateTime.of(2026, 11, 27, 10, 0, 0));
        event.setEventEnd(null);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(42L)).thenReturn(Optional.of(event));

        PortalEventCalendarService.CalendarDownload dl = service.generateIcs(10L, 42L, "es-MX");
        assertThat(dl.content())
                .contains("DTSTART;TZID=America/Mexico_City:20261127T100000")
                .contains("DTEND;TZID=America/Mexico_City:20261127T110000");
    }

    @Test
    void generateIcs_shouldRejectNonEventDraftExpiredAndOutOfAudience() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(42L)).thenReturn(Optional.of(event));

        // Non-event
        event.setContentType(PortalContentType.NEWS);
        assertThatThrownBy(() -> service.generateIcs(10L, 42L, "es-MX")).isInstanceOf(ResourceNotFoundException.class);

        // Draft
        event.setContentType(PortalContentType.EVENT);
        event.setStatus(PortalContentStatus.DRAFT);
        assertThatThrownBy(() -> service.generateIcs(10L, 42L, "es-MX")).isInstanceOf(ResourceNotFoundException.class);

        // Expired publish window
        event.setStatus(PortalContentStatus.PUBLISHED);
        event.setPublishTo(LocalDateTime.now().minusMinutes(5));
        assertThatThrownBy(() -> service.generateIcs(10L, 42L, "es-MX")).isInstanceOf(ResourceNotFoundException.class);

        // Out of audience
        event.setPublishTo(null);
        event.setBusinessUnits(Set.of(BusinessUnitEntity.builder().id(99L).code("OTHER").enabled(true).build()));
        assertThatThrownBy(() -> service.generateIcs(10L, 42L, "es-MX")).isInstanceOf(ResourceNotFoundException.class);

        // Role mismatch
        event.setBusinessUnits(Set.of());
        event.setRoles(Set.of(RoleEntity.builder().id(88L).code("EXEC").build()));
        assertThatThrownBy(() -> service.generateIcs(10L, 42L, "es-MX")).isInstanceOf(ResourceNotFoundException.class);

        // Past event
        event.setRoles(Set.of());
        event.setEventStart(LocalDateTime.now().minusDays(2));
        event.setEventEnd(LocalDateTime.now().minusDays(1));
        assertThatThrownBy(() -> service.generateIcs(10L, 42L, "es-MX")).isInstanceOf(ResourceNotFoundException.class);

        // Scheduled future publishFrom
        event.setEventStart(LocalDateTime.now().plusDays(2));
        event.setEventEnd(null);
        event.setPublishFrom(LocalDateTime.now().plusDays(1));
        assertThatThrownBy(() -> service.generateIcs(10L, 42L, "es-MX")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateIcs_withHasAllBUsAndSpecialChars_shouldSucceed() {
        user.setHasAllBusinessUnits(true);
        event.setBusinessUnits(Set.of(BusinessUnitEntity.builder().id(99L).code("OTHER").enabled(true).build()));
        event.setTeamsUrl(null);
        event.setTranslations(Set.of(
                PortalContentTranslationEntity.builder()
                        .portalContent(event)
                        .language("es-MX")
                        .title("Reunión; Trimestral, 2026\\Q4")
                        .summary("Línea 1\nLínea 2")
                        .build()
        ));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(42L)).thenReturn(Optional.of(event));

        PortalEventCalendarService.CalendarDownload dl = service.generateIcs(10L, 42L, null);
        assertThat(dl.content())
                .contains("SUMMARY:Reunión\\; Trimestral\\, 2026\\\\Q4")
                .contains("DESCRIPTION:Línea 1\\nLínea 2")
                .contains("URL:https://rassini.sharepoint.com/events/townhall");
    }

    @Test
    void generateIcs_minimalEventWithLongTitleAndNoOptionalFields_shouldSucceed() {
        event.setEventLocation(null);
        event.setTeamsUrl(null);
        event.setLinkUrl("/internal/route");
        event.setTranslations(Set.of(
                PortalContentTranslationEntity.builder()
                        .portalContent(event)
                        .language("es-MX")
                        .title("Evento Corporativo Anual Muy Largo Para Validar Truncamiento De Nombre De Archivo")
                        .summary(null)
                        .build()
        ));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(42L)).thenReturn(Optional.of(event));

        PortalEventCalendarService.CalendarDownload dl = service.generateIcs(10L, 42L, "en");
        assertThat(dl.filename()).endsWith("-42.ics").hasSizeLessThanOrEqualTo(58);
        assertThat(dl.content()).doesNotContain("LOCATION:").doesNotContain("URL:").doesNotContain("DESCRIPTION:");
    }
}
