package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.config.PortalHomeProperties;
import com.rassini.employeeportal.dto.response.HomeItemResponse;
import com.rassini.employeeportal.dto.response.HomeResponse;
import com.rassini.employeeportal.entity.*;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.PortalContentMapper;
import com.rassini.employeeportal.repository.PortalContentRepository;
import com.rassini.employeeportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PortalHomeServiceImplTest {

    @Mock private PortalContentRepository contentRepository;
    @Mock private UserRepository userRepository;

    private PortalHomeServiceImpl service;
    private BusinessUnitEntity bu1;
    private BusinessUnitEntity bu2;
    private RoleEntity roleEmp;
    private RoleEntity roleMgr;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PortalHomeServiceImpl(contentRepository, userRepository, new PortalContentMapper(), new PortalHomeProperties());
        bu1 = BusinessUnitEntity.builder().id(1L).code("BU1").name("Planta 1").enabled(true).build();
        bu2 = BusinessUnitEntity.builder().id(2L).code("BU2").name("Planta 2").enabled(true).build();
        roleEmp = RoleEntity.builder().id(10L).code("EMP").build();
        roleMgr = RoleEntity.builder().id(20L).code("MGR").build();
    }

    private PortalContentEntity item(Long id, PortalContentType type, int prio, Set<BusinessUnitEntity> bus, Set<RoleEntity> roles,
                                     String titleEs, String titleEn) {
        PortalContentEntity e = PortalContentEntity.builder()
                .id(id).contentType(type).status(PortalContentStatus.PUBLISHED).priority(prio)
                .publishFrom(LocalDateTime.now().minusDays(1))
                .businessUnits(bus).roles(roles)
                .build();
        if (titleEs != null) {
            e.getTranslations().add(PortalContentTranslationEntity.builder()
                    .language("es-MX").title(titleEs).summary("Sum ES").body("Body ES").build());
        }
        if (titleEn != null) {
            e.getTranslations().add(PortalContentTranslationEntity.builder()
                    .language("en-US").title(titleEn).summary("Sum EN").body("Body EN").build());
        }
        return e;
    }

    @Test
    void getHome_filtersByAudienceLanguageAndHeroOverflow() {
        UserEntity user = UserEntity.builder()
                .id(1L).username("jdoe").hasAllBusinessUnits(false)
                .businessUnits(Set.of(bu1)).roles(Set.of(roleEmp)).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        PortalContentEntity hero1 = item(1L, PortalContentType.HERO_NEWS, 1, Set.of(), Set.of(), "Hero 1 ES", "Hero 1 EN");
        PortalContentEntity hero2 = item(2L, PortalContentType.HERO_NEWS, 2, Set.of(), Set.of(), "Hero 2 ES", null); // fallback to ES
        PortalContentEntity newsBu1 = item(3L, PortalContentType.NEWS, 1, Set.of(bu1), Set.of(), "News BU1", null);
        PortalContentEntity newsBu2 = item(4L, PortalContentType.NEWS, 1, Set.of(bu2), Set.of(), "News BU2", null); // excluded
        PortalContentEntity noticeBothMatch = item(5L, PortalContentType.NOTICE, 1, Set.of(bu1), Set.of(roleEmp), "Notice OK", null);
        PortalContentEntity noticeRoleMiss = item(6L, PortalContentType.NOTICE, 1, Set.of(bu1), Set.of(roleMgr), "Notice NO", null);
        PortalContentEntity futureEvent = item(7L, PortalContentType.EVENT, 1, Set.of(), Set.of(), "Future Event", null);
        futureEvent.setEventStart(LocalDateTime.now().plusDays(3));
        PortalContentEntity pastEvent = item(8L, PortalContentType.EVENT, 1, Set.of(), Set.of(), "Past Event", null);
        pastEvent.setEventStart(LocalDateTime.now().minusDays(2));

        when(contentRepository.findActivePublished(eq(PortalContentStatus.PUBLISHED), any()))
                .thenReturn(List.of(hero1, hero2, newsBu1, newsBu2, noticeBothMatch, noticeRoleMiss, futureEvent, pastEvent));

        HomeResponse home = service.getHome(1L, "en");
        assertEquals("en-US", home.getRequestedLanguage());
        assertNotNull(home.getHero());
        assertEquals("Hero 1 EN", home.getHero().getTitle());
        assertNull(home.getHero().getBody());
        assertTrue(home.getHero().isHasBody());

        // hero2 overflows into secondaryNews with ES fallback + newsBu1
        assertEquals(2, home.getSecondaryNews().size());
        assertEquals("Hero 2 ES", home.getSecondaryNews().get(0).getTitle());
        assertEquals("es-MX", home.getSecondaryNews().get(0).getLanguage());
        assertEquals("News BU1", home.getSecondaryNews().get(1).getTitle());

        assertEquals(1, home.getNotices().size());
        assertEquals("Notice OK", home.getNotices().get(0).getTitle());

        assertEquals(1, home.getEvents().size());
        assertEquals("Future Event", home.getEvents().get(0).getTitle());
    }

    @Test
    void getHome_hasAllBusinessUnitsSeesAnyBuAndDetailIncludesBody() {
        UserEntity globalUser = UserEntity.builder()
                .id(2L).username("global").hasAllBusinessUnits(true)
                .businessUnits(Set.of()).roles(Set.of(roleEmp)).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(globalUser));

        PortalContentEntity newsBu2 = item(4L, PortalContentType.NEWS, 1, Set.of(bu2), Set.of(), "News BU2", null);
        when(contentRepository.findActivePublished(eq(PortalContentStatus.PUBLISHED), any())).thenReturn(List.of(newsBu2));

        HomeResponse home = service.getHome(2L, null);
        assertEquals("es-MX", home.getRequestedLanguage());
        assertEquals(1, home.getSecondaryNews().size());

        when(contentRepository.findById(4L)).thenReturn(Optional.of(newsBu2));
        HomeItemResponse detail = service.getHomeItemDetail(2L, 4L, "es-MX");
        assertEquals("Body ES", detail.getBody());

        PortalContentEntity draft = item(9L, PortalContentType.NEWS, 1, Set.of(), Set.of(), "Draft", null);
        draft.setStatus(PortalContentStatus.DRAFT);
        when(contentRepository.findById(9L)).thenReturn(Optional.of(draft));
        assertThrows(ResourceNotFoundException.class, () -> service.getHomeItemDetail(2L, 9L, "es-MX"));

        PortalContentEntity futurePub = item(10L, PortalContentType.NEWS, 1, Set.of(), Set.of(), "Future", null);
        futurePub.setPublishFrom(LocalDateTime.now().plusDays(1));
        when(contentRepository.findById(10L)).thenReturn(Optional.of(futurePub));
        assertThrows(ResourceNotFoundException.class, () -> service.getHomeItemDetail(2L, 10L, "es-MX"));

        PortalContentEntity expiredPub = item(11L, PortalContentType.NEWS, 1, Set.of(), Set.of(), "Expired", null);
        expiredPub.setPublishTo(LocalDateTime.now().minusMinutes(5));
        when(contentRepository.findById(11L)).thenReturn(Optional.of(expiredPub));
        assertThrows(ResourceNotFoundException.class, () -> service.getHomeItemDetail(2L, 11L, "es-MX"));

        PortalContentEntity noTrans = item(12L, PortalContentType.NEWS, 1, Set.of(), Set.of(), null, null);
        when(contentRepository.findById(12L)).thenReturn(Optional.of(noTrans));
        assertThrows(ResourceNotFoundException.class, () -> service.getHomeItemDetail(2L, 12L, "es-MX"));

        PortalContentEntity onlyEn = item(13L, PortalContentType.NEWS, 1, Set.of(), Set.of(), null, "Only EN");
        when(contentRepository.findById(13L)).thenReturn(Optional.of(onlyEn));
        assertThrows(ResourceNotFoundException.class, () -> service.getHomeItemDetail(2L, 13L, "es-MX"));

        UserEntity nullSetsUser = UserEntity.builder().id(3L).username("u3").hasAllBusinessUnits(null).businessUnits(null).roles(null).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(nullSetsUser));
        when(contentRepository.findActivePublished(eq(PortalContentStatus.PUBLISHED), any()))
                .thenReturn(List.of(
                        item(21L, PortalContentType.NEWS, 1, Set.of(), Set.of(), "N1", null),
                        item(22L, PortalContentType.NEWS, 2, Set.of(), Set.of(), "N2", null),
                        item(23L, PortalContentType.NEWS, 3, Set.of(), Set.of(), "N3", null),
                        item(24L, PortalContentType.NEWS, 4, Set.of(), Set.of(), "N4", null),
                        item(25L, PortalContentType.NEWS, 5, Set.of(), Set.of(), "N5", null)
                ));
        HomeResponse limited = service.getHome(3L, "es-MX");
        assertEquals(4, limited.getSecondaryNews().size());
    }
}
