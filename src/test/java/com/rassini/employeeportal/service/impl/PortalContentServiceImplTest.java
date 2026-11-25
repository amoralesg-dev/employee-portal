package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.PortalContentRequest;
import com.rassini.employeeportal.dto.response.AudienceOptionsResponse;
import com.rassini.employeeportal.dto.response.PortalContentResponse;
import com.rassini.employeeportal.entity.*;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.PortalContentMapper;
import com.rassini.employeeportal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PortalContentServiceImplTest {

    @Mock private PortalContentRepository contentRepository;
    @Mock private PortalMediaRepository mediaRepository;
    @Mock private BusinessUnitRepository businessUnitRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;

    private PortalContentServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PortalContentServiceImpl(
                contentRepository, mediaRepository, businessUnitRepository, roleRepository, userRepository, new PortalContentMapper());
    }

    private PortalContentRequest validRequest() {
        return PortalContentRequest.builder()
                .contentType(PortalContentType.NEWS)
                .category(PortalContentCategory.CULTURA)
                .priority(10)
                .linkUrl("https://rss-cxp.rassini.com/portal-cxp")
                .linkNewTab(true)
                .icon("pi pi-megaphone")
                .businessUnitIds(List.of(1L))
                .roleIds(List.of(2L))
                .translations(List.of(
                        PortalContentRequest.TranslationRequest.builder()
                                .language("es-MX").title("Título ES").summary("Resumen").body("Cuerpo").linkLabel("Ver").build(),
                        PortalContentRequest.TranslationRequest.builder()
                                .language("en-US").title("Title EN").summary("Summary").body("Body").linkLabel("View").build()
                ))
                .build();
    }

    @Test
    void createUpdateSearchAndAudienceOptions() {
        UserEntity user = UserEntity.builder().id(1L).username("comms").build();
        BusinessUnitEntity bu = BusinessUnitEntity.builder().id(1L).code("PLANTA1").name("Planta 1").enabled(true).build();
        RoleEntity role = RoleEntity.builder().id(2L).code("EMPLOYEE").name("Empleado").build();
        PortalMediaEntity media = PortalMediaEntity.builder().id(7L).storageKey("a.png").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(businessUnitRepository.findAllById(List.of(1L))).thenReturn(List.of(bu));
        when(roleRepository.findAllById(List.of(2L))).thenReturn(List.of(role));
        when(mediaRepository.findById(7L)).thenReturn(Optional.of(media));
        when(contentRepository.save(any())).thenAnswer(inv -> {
            PortalContentEntity e = inv.getArgument(0);
            if (e.getId() == null) e.setId(50L);
            return e;
        });

        PortalContentRequest req = validRequest();
        req.setImageId(7L);
        PortalContentResponse created = service.create(req, 1L);
        assertEquals(50L, created.getId());
        assertEquals(PortalContentStatus.DRAFT, created.getStatus());
        assertEquals("DRAFT", created.getDisplayStatus());
        assertEquals(2, created.getTranslations().size());
        assertEquals(7L, created.getImageId());

        PortalContentEntity existing = PortalContentEntity.builder()
                .id(50L).contentType(PortalContentType.NEWS).status(PortalContentStatus.DRAFT).translations(new HashSet<>()).build();
        when(contentRepository.findById(50L)).thenReturn(Optional.of(existing));

        PortalContentRequest evReq = validRequest();
        evReq.setContentType(PortalContentType.EVENT);
        evReq.setPriority(null);
        evReq.setLinkUrl("   ");
        evReq.setIcon("   ");
        evReq.setBusinessUnitIds(null);
        evReq.setRoleIds(null);
        evReq.setEvent(PortalContentRequest.EventRequest.builder()
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .location("Auditorio").build());
        PortalContentResponse updated = service.update(50L, evReq, null);
        assertEquals(50L, updated.getId());
        assertEquals(100, updated.getPriority());

        when(contentRepository.searchAdmin(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(existing)));
        Page<PortalContentResponse> page = service.searchAdmin(null, null, null, null, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());

        assertEquals(50L, service.getById(50L).getId());

        RoleEntity roleNoName = RoleEntity.builder().id(3L).code("ADMIN").name(null).build();
        when(businessUnitRepository.findByEnabledTrue()).thenReturn(List.of(bu));
        when(roleRepository.findAll()).thenReturn(List.of(role, roleNoName));
        AudienceOptionsResponse opts = service.getAudienceOptions();
        assertEquals(1, opts.getBusinessUnits().size());
        assertEquals(2, opts.getRoles().size());
    }

    @Test
    void statusTransitionsAndDeleteRules() {
        PortalContentEntity entity = PortalContentEntity.builder()
                .id(5L)
                .contentType(PortalContentType.NEWS)
                .status(PortalContentStatus.DRAFT)
                .translations(new HashSet<>(Set.of(
                        PortalContentTranslationEntity.builder().language("es-MX").title("Hola").build())))
                .build();
        when(contentRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(contentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Same status no-op
        assertEquals(PortalContentStatus.DRAFT, service.updateStatus(5L, PortalContentStatus.DRAFT, null).getStatus());

        // DRAFT -> INACTIVE not allowed
        assertThrows(ResponseStatusException.class, () -> service.updateStatus(5L, PortalContentStatus.INACTIVE, null));

        // DRAFT -> PUBLISHED allowed
        PortalContentResponse pub = service.updateStatus(5L, PortalContentStatus.PUBLISHED, 1L);
        assertEquals(PortalContentStatus.PUBLISHED, pub.getStatus());
        assertEquals("ACTIVE", pub.getDisplayStatus());

        // Cannot delete PUBLISHED
        assertThrows(ResponseStatusException.class, () -> service.delete(5L));

        // PUBLISHED -> DRAFT not allowed
        assertThrows(ResponseStatusException.class, () -> service.updateStatus(5L, PortalContentStatus.DRAFT, null));

        // PUBLISHED -> INACTIVE -> PUBLISHED -> INACTIVE -> DRAFT -> delete
        service.updateStatus(5L, PortalContentStatus.INACTIVE, null);
        service.updateStatus(5L, PortalContentStatus.PUBLISHED, null);
        service.updateStatus(5L, PortalContentStatus.INACTIVE, null);
        service.updateStatus(5L, PortalContentStatus.DRAFT, null);
        service.delete(5L);
        verify(contentRepository).delete(entity);

        // Publish without ES title or bad dates
        PortalContentEntity badEntity = PortalContentEntity.builder()
                .id(6L).contentType(PortalContentType.NEWS).status(PortalContentStatus.DRAFT).translations(new HashSet<>()).build();
        when(contentRepository.findById(6L)).thenReturn(Optional.of(badEntity));
        assertThrows(BusinessException.class, () -> service.updateStatus(6L, PortalContentStatus.PUBLISHED, null));

        badEntity.getTranslations().add(PortalContentTranslationEntity.builder().language("es-MX").title("OK").build());
        badEntity.setPublishFrom(LocalDateTime.now().plusDays(2));
        badEntity.setPublishTo(LocalDateTime.now().plusDays(1));
        assertThrows(BusinessException.class, () -> service.updateStatus(6L, PortalContentStatus.PUBLISHED, null));
    }

    @Test
    void validationErrors() {
        PortalContentRequest badDates = validRequest();
        badDates.setPublishFrom(LocalDateTime.now().plusDays(2));
        badDates.setPublishTo(LocalDateTime.now().plusDays(1));
        assertThrows(BusinessException.class, () -> service.create(badDates, null));

        PortalContentRequest badUrl = validRequest();
        badUrl.setLinkUrl("javascript:alert(1)");
        assertThrows(BusinessException.class, () -> service.create(badUrl, null));

        PortalContentRequest relUrl = validRequest();
        relUrl.setLinkUrl("/pagos");
        relUrl.setBusinessUnitIds(List.of());
        relUrl.setRoleIds(List.of());
        relUrl.setPublishFrom(LocalDateTime.now().minusDays(1));
        relUrl.setPublishTo(LocalDateTime.now().plusDays(1));
        when(contentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertNotNull(service.create(relUrl, null));

        PortalContentRequest longSharePointUrl = validRequest();
        longSharePointUrl.setLinkUrl("https://rassiniauto.sharepoint.com/" + "a".repeat(500));
        longSharePointUrl.setBusinessUnitIds(List.of());
        longSharePointUrl.setRoleIds(List.of());
        assertNotNull(service.create(longSharePointUrl, null));

        PortalContentRequest protoRelUrl = validRequest();
        protoRelUrl.setLinkUrl("//evil.com");
        assertThrows(BusinessException.class, () -> service.create(protoRelUrl, null));

        PortalContentRequest invalidUri = validRequest();
        invalidUri.setLinkUrl("http://[bad uri");
        assertThrows(BusinessException.class, () -> service.create(invalidUri, null));

        PortalContentRequest noEs = validRequest();
        noEs.setTranslations(List.of(
                PortalContentRequest.TranslationRequest.builder().language("en-US").title("Only EN").build()));
        assertThrows(BusinessException.class, () -> service.create(noEs, null));

        PortalContentRequest badLang = validRequest();
        badLang.setTranslations(List.of(
                PortalContentRequest.TranslationRequest.builder().language("fr-FR").title("Bonjour").build()));
        assertThrows(BusinessException.class, () -> service.create(badLang, null));

        PortalContentRequest dupLang = validRequest();
        dupLang.setTranslations(List.of(
                PortalContentRequest.TranslationRequest.builder().language("es-MX").title("A").build(),
                PortalContentRequest.TranslationRequest.builder().language("es-MX").title("B").build()));
        assertThrows(BusinessException.class, () -> service.create(dupLang, null));

        PortalContentRequest eventOnNews = validRequest();
        eventOnNews.setEvent(PortalContentRequest.EventRequest.builder().location("Sala").build());
        assertThrows(BusinessException.class, () -> service.create(eventOnNews, null));

        PortalContentRequest badEventDates = validRequest();
        badEventDates.setContentType(PortalContentType.EVENT);
        badEventDates.setEvent(PortalContentRequest.EventRequest.builder()
                .start(LocalDateTime.now().plusDays(3)).end(LocalDateTime.now().plusDays(1)).build());
        assertThrows(BusinessException.class, () -> service.create(badEventDates, null));

        PortalContentRequest eventNoStart = validRequest();
        eventNoStart.setContentType(PortalContentType.EVENT);
        eventNoStart.setEvent(null);
        assertThrows(BusinessException.class, () -> service.create(eventNoStart, null));

        PortalContentRequest qaNoLink = validRequest();
        qaNoLink.setContentType(PortalContentType.QUICK_ACCESS);
        qaNoLink.setLinkUrl("  ");
        assertThrows(BusinessException.class, () -> service.create(qaNoLink, null));

        PortalContentRequest missingBu = validRequest();
        when(businessUnitRepository.findAllById(List.of(1L))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.create(missingBu, null));

        PortalContentRequest missingRole = validRequest();
        when(businessUnitRepository.findAllById(List.of(1L))).thenReturn(List.of(BusinessUnitEntity.builder().id(1L).build()));
        when(roleRepository.findAllById(List.of(2L))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.create(missingRole, null));

        PortalContentRequest missingImg = validRequest();
        missingImg.setImageId(999L);
        when(mediaRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.create(missingImg, null));

        when(contentRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(999L));
    }

    @Test
    void update_mergesTranslationsInPlacePreservingIdsAndRemovingOrphans() {
        PortalMediaEntity media = PortalMediaEntity.builder().id(2L).storageKey("img.png").build();
        PortalContentTranslationEntity existingEs = PortalContentTranslationEntity.builder()
                .id(101L).language("es-MX").title("Original ES").summary("Old").body("OldBody").linkLabel("OldLbl").build();
        PortalContentTranslationEntity existingEn = PortalContentTranslationEntity.builder()
                .id(102L).language("en-US").title("Original EN").summary("Old EN").build();

        PortalContentEntity existing = PortalContentEntity.builder()
                .id(1L).contentType(PortalContentType.HERO_NEWS).status(PortalContentStatus.INACTIVE)
                .image(media).linkUrl("https://rassiniauto.sharepoint.com/" + "x".repeat(500))
                .translations(new HashSet<>(Set.of(existingEs, existingEn)))
                .build();
        existingEs.setPortalContent(existing);
        existingEn.setPortalContent(existing);

        when(contentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(mediaRepository.findById(2L)).thenReturn(Optional.of(media));
        when(contentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // A & B: Update both es-MX and en-US -> preserves same IDs (101L, 102L), linkUrl & imageId
        PortalContentRequest updateBoth = PortalContentRequest.builder()
                .contentType(PortalContentType.HERO_NEWS)
                .priority(1)
                .imageId(2L)
                .linkUrl("https://rassiniauto.sharepoint.com/" + "x".repeat(500))
                .translations(List.of(
                        PortalContentRequest.TranslationRequest.builder()
                                .language("es-MX").title("Editado ES").summary("Nuevo").body("Cuerpo nuevo").linkLabel("Abrir").build(),
                        PortalContentRequest.TranslationRequest.builder()
                                .language("en-US").title("Edited EN").summary("New EN").build()
                ))
                .build();

        service.update(1L, updateBoth, null);
        assertEquals(2, existing.getTranslations().size());
        assertSame(existingEs, existing.getTranslations().stream().filter(t -> "es-MX".equals(t.getLanguage())).findFirst().orElseThrow());
        assertEquals(101L, existingEs.getId());
        assertEquals("Editado ES", existingEs.getTitle());
        assertEquals(102L, existingEn.getId());
        assertEquals("Edited EN", existingEn.getTitle());
        assertEquals(2L, existing.getImage().getId());

        // D: Remove en-US -> only es-MX remains with ID 101L
        PortalContentRequest onlyEs = PortalContentRequest.builder()
                .contentType(PortalContentType.HERO_NEWS)
                .priority(1)
                .imageId(2L)
                .linkUrl("https://rassiniauto.sharepoint.com/" + "x".repeat(500))
                .translations(List.of(
                        PortalContentRequest.TranslationRequest.builder().language("es-MX").title("Solo ES").build()
                ))
                .build();
        service.update(1L, onlyEs, null);
        assertEquals(1, existing.getTranslations().size());
        assertEquals(101L, existing.getTranslations().iterator().next().getId());

        // C: Re-add en-US -> es-MX keeps ID 101L, new en-US added
        service.update(1L, updateBoth, null);
        assertEquals(2, existing.getTranslations().size());
        assertEquals(101L, existing.getTranslations().stream().filter(t -> "es-MX".equals(t.getLanguage())).findFirst().orElseThrow().getId());
        assertNull(existing.getTranslations().stream().filter(t -> "en-US".equals(t.getLanguage())).findFirst().orElseThrow().getId());
    }

    @Test
    void createEvent_withValidTeamsUrl_shouldPersistTeamsUrl() {
        when(contentRepository.save(any())).thenAnswer(inv -> {
            PortalContentEntity e = inv.getArgument(0);
            e.setId(77L);
            return e;
        });

        PortalContentRequest req = PortalContentRequest.builder()
                .contentType(PortalContentType.EVENT)
                .event(PortalContentRequest.EventRequest.builder()
                        .start(LocalDateTime.now().plusDays(1))
                        .end(LocalDateTime.now().plusDays(1).plusHours(1))
                        .location("Auditorio")
                        .teamsUrl("https://teams.microsoft.com/l/meetup-join/valid")
                        .build())
                .translations(List.of(PortalContentRequest.TranslationRequest.builder()
                        .language("es-MX").title("Evento Teams").build()))
                .build();

        PortalContentResponse res = service.create(req, null);
        assertEquals("https://teams.microsoft.com/l/meetup-join/valid", res.getEvent().getTeamsUrl());
    }

    @Test
    void createEvent_withInvalidOrTooLongTeamsUrl_shouldThrowBusinessException() {
        List<String> invalidUrls = List.of(
                "http://teams.microsoft.com/insecure",
                "javascript:alert(1)",
                "data:text/html,hi",
                "file:///etc/passwd",
                "/relative/path",
                "//teams.microsoft.com/proto",
                "https://teams.microsoft.com/" + "a".repeat(2000)
        );
        for (String bad : invalidUrls) {
            PortalContentRequest req = PortalContentRequest.builder()
                    .contentType(PortalContentType.EVENT)
                    .event(PortalContentRequest.EventRequest.builder()
                            .start(LocalDateTime.now().plusDays(1))
                            .teamsUrl(bad)
                            .build())
                    .translations(List.of(PortalContentRequest.TranslationRequest.builder()
                            .language("es-MX").title("Evento").build()))
                    .build();
            assertThrows(BusinessException.class, () -> service.create(req, null), "Should reject: " + bad);
        }
    }
}
