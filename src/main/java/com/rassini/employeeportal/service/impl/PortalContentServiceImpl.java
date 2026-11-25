package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.PortalContentRequest;
import com.rassini.employeeportal.dto.response.AudienceOptionsResponse;
import com.rassini.employeeportal.dto.response.PortalContentResponse;
import com.rassini.employeeportal.entity.*;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.PortalContentMapper;
import com.rassini.employeeportal.repository.*;
import com.rassini.employeeportal.service.PortalContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PortalContentServiceImpl implements PortalContentService {

    private static final String LANG_ES = "es-MX";
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("es-MX", "en-US");

    private final PortalContentRepository contentRepository;
    private final PortalMediaRepository mediaRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PortalContentMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<PortalContentResponse> searchAdmin(
            PortalContentType type,
            PortalContentStatus status,
            PortalContentCategory category,
            String q,
            Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return contentRepository.searchAdmin(type, status, category, q, pageable)
                .map(c -> mapper.toAdminResponse(c, now));
    }

    @Override
    @Transactional(readOnly = true)
    public PortalContentResponse getById(Long id) {
        PortalContentEntity entity = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido", "id", id));
        return mapper.toAdminResponse(entity, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public AudienceOptionsResponse getAudienceOptions() {
        List<AudienceOptionsResponse.OptionItem> bus = businessUnitRepository.findByEnabledTrue().stream()
                .sorted(Comparator.comparing(BusinessUnitEntity::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(b -> AudienceOptionsResponse.OptionItem.builder()
                        .id(b.getId())
                        .code(b.getCode())
                        .name(b.getName())
                        .build())
                .toList();

        List<AudienceOptionsResponse.OptionItem> roles = roleRepository.findAll().stream()
                .sorted(Comparator.comparing(RoleEntity::getCode, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(r -> AudienceOptionsResponse.OptionItem.builder()
                        .id(r.getId())
                        .code(r.getCode())
                        .name(r.getName() != null ? r.getName() : r.getCode())
                        .build())
                .toList();

        return AudienceOptionsResponse.builder()
                .businessUnits(bus)
                .roles(roles)
                .build();
    }

    @Override
    @Transactional
    public PortalContentResponse create(PortalContentRequest request, Long userId) {
        validateRequest(request);
        UserEntity actor = userId != null ? userRepository.findById(userId).orElse(null) : null;

        PortalContentEntity entity = PortalContentEntity.builder()
                .status(PortalContentStatus.DRAFT)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        applyFields(entity, request);
        PortalContentEntity saved = contentRepository.save(entity);
        return mapper.toAdminResponse(saved, LocalDateTime.now());
    }

    @Override
    @Transactional
    public PortalContentResponse update(Long id, PortalContentRequest request, Long userId) {
        validateRequest(request);
        PortalContentEntity entity = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido", "id", id));

        UserEntity actor = userId != null ? userRepository.findById(userId).orElse(null) : null;
        entity.setUpdatedBy(actor);
        applyFields(entity, request);

        PortalContentEntity saved = contentRepository.save(entity);
        return mapper.toAdminResponse(saved, LocalDateTime.now());
    }

    @Override
    @Transactional
    public PortalContentResponse updateStatus(Long id, PortalContentStatus targetStatus, Long userId) {
        PortalContentEntity entity = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido", "id", id));

        PortalContentStatus current = entity.getStatus();
        if (current == targetStatus) {
            return mapper.toAdminResponse(entity, LocalDateTime.now());
        }

        validateTransition(current, targetStatus);

        if (targetStatus == PortalContentStatus.PUBLISHED) {
            boolean hasEsTitle = entity.getTranslations().stream()
                    .anyMatch(t -> LANG_ES.equals(t.getLanguage()) && t.getTitle() != null && !t.getTitle().isBlank());
            if (!hasEsTitle) {
                throw new BusinessException("No se puede publicar sin una traducción en español (es-MX) con título");
            }
            if (entity.getPublishFrom() != null && entity.getPublishTo() != null
                    && !entity.getPublishTo().isAfter(entity.getPublishFrom())) {
                throw new BusinessException("La fecha de fin de publicación debe ser posterior a la fecha de inicio");
            }
        }

        UserEntity actor = userId != null ? userRepository.findById(userId).orElse(null) : null;
        entity.setStatus(targetStatus);
        entity.setUpdatedBy(actor);

        PortalContentEntity saved = contentRepository.save(entity);
        return mapper.toAdminResponse(saved, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PortalContentEntity entity = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido", "id", id));

        if (entity.getStatus() == PortalContentStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar un contenido publicado. Despublíquelo primero.");
        }
        contentRepository.delete(entity);
    }

    private void validateTransition(PortalContentStatus current, PortalContentStatus target) {
        boolean valid = switch (current) {
            case DRAFT -> target == PortalContentStatus.PUBLISHED;
            case PUBLISHED -> target == PortalContentStatus.INACTIVE;
            case INACTIVE -> target == PortalContentStatus.PUBLISHED || target == PortalContentStatus.DRAFT;
        };
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transición de estado no permitida: " + current + " -> " + target);
        }
    }

    private void validateRequest(PortalContentRequest request) {
        if (request.getPublishFrom() != null && request.getPublishTo() != null
                && request.getPublishTo().isBefore(request.getPublishFrom())) {
            throw new BusinessException("La fecha de fin de publicación no puede ser anterior a la fecha de inicio");
        }

        if (request.getContentType() == PortalContentType.QUICK_ACCESS
                && (request.getLinkUrl() == null || request.getLinkUrl().isBlank())) {
            throw new BusinessException("El enlace es obligatorio para contenido de tipo Acceso rápido");
        }

        if (request.getContentType() == PortalContentType.EVENT) {
            if (request.getEvent() == null || request.getEvent().getStart() == null) {
                throw new BusinessException("La fecha de inicio del evento es obligatoria");
            }
            if (request.getEvent().getEnd() != null
                    && request.getEvent().getEnd().isBefore(request.getEvent().getStart())) {
                throw new BusinessException("La fecha de fin del evento no puede ser anterior a la fecha de inicio");
            }
            validateTeamsUrl(request.getEvent().getTeamsUrl());
        } else if (request.getEvent() != null) {
            boolean hasEventData = request.getEvent().getStart() != null
                    || request.getEvent().getEnd() != null
                    || (request.getEvent().getLocation() != null && !request.getEvent().getLocation().isBlank())
                    || (request.getEvent().getTeamsUrl() != null && !request.getEvent().getTeamsUrl().isBlank());
            if (hasEventData) {
                throw new BusinessException("Los datos de evento solo son válidos para contenido de tipo EVENT");
            }
        }

        validateLinkUrl(request.getLinkUrl());

        Set<String> seenLanguages = new HashSet<>();
        boolean hasEs = false;
        for (PortalContentRequest.TranslationRequest tr : request.getTranslations()) {
            String lang = tr.getLanguage().trim();
            if (!SUPPORTED_LANGUAGES.contains(lang)) {
                throw new BusinessException("Idioma no soportado: " + lang + ". Use es-MX o en-US");
            }
            if (!seenLanguages.add(lang)) {
                throw new BusinessException("Idioma duplicado en las traducciones: " + lang);
            }
            if (LANG_ES.equals(lang) && tr.getTitle() != null && !tr.getTitle().isBlank()) {
                hasEs = true;
            }
        }
        if (!hasEs) {
            throw new BusinessException("La traducción en español (es-MX) con título es obligatoria");
        }
    }

    private void validateLinkUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("//")) {
            throw new BusinessException("URL de enlace no permitida");
        }
        if (trimmed.startsWith("/")) {
            return;
        }
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new BusinessException("Solo se permiten rutas relativas o URLs http/https");
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException("URL de enlace inválida");
        }
    }

    private void validateTeamsUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.length() > 2000) {
            throw new BusinessException("La URL de Microsoft Teams no debe exceder 2000 caracteres");
        }
        if (trimmed.startsWith("//") || trimmed.startsWith("/")) {
            throw new BusinessException("La URL de Microsoft Teams debe usar exclusivamente HTTPS");
        }
        try {
            URI uri = URI.create(trimmed);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new BusinessException("La URL de Microsoft Teams debe usar exclusivamente HTTPS");
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException("La URL de Microsoft Teams no es válida");
        }
    }

    private void applyFields(PortalContentEntity entity, PortalContentRequest request) {
        entity.setContentType(request.getContentType());
        entity.setCategory(request.getCategory());
        entity.setPriority(request.getPriority() != null ? request.getPriority() : 100);
        entity.setPublishFrom(request.getPublishFrom());
        entity.setPublishTo(request.getPublishTo());
        entity.setLinkUrl(request.getLinkUrl() != null && !request.getLinkUrl().isBlank() ? request.getLinkUrl().trim() : null);
        entity.setLinkNewTab(Boolean.TRUE.equals(request.getLinkNewTab()));
        entity.setIcon(request.getIcon() != null && !request.getIcon().isBlank() ? request.getIcon().trim() : null);

        if (request.getImageId() != null) {
            PortalMediaEntity media = mediaRepository.findById(request.getImageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medio", "id", request.getImageId()));
            entity.setImage(media);
        } else {
            entity.setImage(null);
        }

        if (request.getContentType() == PortalContentType.EVENT && request.getEvent() != null) {
            entity.setEventStart(request.getEvent().getStart());
            entity.setEventEnd(request.getEvent().getEnd());
            entity.setEventLocation(request.getEvent().getLocation());
            String teamsUrl = request.getEvent().getTeamsUrl();
            entity.setTeamsUrl(teamsUrl != null && !teamsUrl.isBlank() ? teamsUrl.trim() : null);
        } else {
            entity.setEventStart(null);
            entity.setEventEnd(null);
            entity.setEventLocation(null);
            entity.setTeamsUrl(null);
        }

        // Audiencia Business Units
        Set<BusinessUnitEntity> bus = new HashSet<>();
        if (request.getBusinessUnitIds() != null && !request.getBusinessUnitIds().isEmpty()) {
            List<BusinessUnitEntity> found = businessUnitRepository.findAllById(request.getBusinessUnitIds());
            if (found.size() != new HashSet<>(request.getBusinessUnitIds()).size()) {
                throw new BusinessException("Una o más Business Units especificadas no existen");
            }
            bus.addAll(found);
        }
        entity.setBusinessUnits(bus);

        // Audiencia Roles
        Set<RoleEntity> roles = new HashSet<>();
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            List<RoleEntity> found = roleRepository.findAllById(request.getRoleIds());
            if (found.size() != new HashSet<>(request.getRoleIds()).size()) {
                throw new BusinessException("Uno o más roles especificados no existen");
            }
            roles.addAll(found);
        }
        entity.setRoles(roles);

        // Traducciones: merge in-place por language (evita colisión INSERT antes de DELETE con uk_portal_content_translation)
        Map<String, PortalContentTranslationEntity> existingByLang = new HashMap<>();
        for (PortalContentTranslationEntity existing : entity.getTranslations()) {
            existingByLang.put(existing.getLanguage(), existing);
        }

        Set<String> incomingLangs = new HashSet<>();
        for (PortalContentRequest.TranslationRequest tr : request.getTranslations()) {
            String lang = tr.getLanguage().trim();
            incomingLangs.add(lang);
            PortalContentTranslationEntity target = existingByLang.get(lang);
            if (target != null) {
                target.setTitle(tr.getTitle().trim());
                target.setSummary(tr.getSummary() != null ? tr.getSummary().trim() : null);
                target.setBody(tr.getBody());
                target.setLinkLabel(tr.getLinkLabel() != null ? tr.getLinkLabel().trim() : null);
            } else {
                entity.getTranslations().add(PortalContentTranslationEntity.builder()
                        .portalContent(entity)
                        .language(lang)
                        .title(tr.getTitle().trim())
                        .summary(tr.getSummary() != null ? tr.getSummary().trim() : null)
                        .body(tr.getBody())
                        .linkLabel(tr.getLinkLabel() != null ? tr.getLinkLabel().trim() : null)
                        .build());
            }
        }
        entity.getTranslations().removeIf(t -> !incomingLangs.contains(t.getLanguage()));
    }
}
