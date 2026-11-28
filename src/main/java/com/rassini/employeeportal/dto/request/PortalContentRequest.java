package com.rassini.employeeportal.dto.request;

import com.rassini.employeeportal.entity.PortalContentCategory;
import com.rassini.employeeportal.entity.PortalContentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalContentRequest {

    @NotNull(message = "El tipo de contenido es obligatorio")
    private PortalContentType contentType;

    private PortalContentCategory category;

    private Integer priority;

    private LocalDateTime publishFrom;

    private LocalDateTime publishTo;

    private Long imageId;

    @Size(max = 2000, message = "La URL del enlace no debe exceder 2000 caracteres")
    private String linkUrl;

    private Boolean linkNewTab;

    @Size(max = 50, message = "El ícono no debe exceder 50 caracteres")
    private String icon;

    @Valid
    private EventRequest event;

    private List<Long> businessUnitIds;

    private List<Long> roleIds;

    @NotEmpty(message = "Debe incluir al menos la traducción en español (es-MX)")
    @Valid
    private List<TranslationRequest> translations;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventRequest {
        private LocalDateTime start;
        private LocalDateTime end;
        @Size(max = 200, message = "La ubicación del evento no debe exceder 200 caracteres")
        private String location;
        @Size(max = 2000, message = "La URL de Microsoft Teams no debe exceder 2000 caracteres")
        private String teamsUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TranslationRequest {
        @NotBlank(message = "El idioma es obligatorio")
        @Size(max = 5, message = "El código de idioma no debe exceder 5 caracteres")
        private String language;

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título no debe exceder 200 caracteres")
        private String title;

        @Size(max = 500, message = "El resumen no debe exceder 500 caracteres")
        private String summary;

        private String body;

        @Size(max = 100, message = "La etiqueta del enlace no debe exceder 100 caracteres")
        private String linkLabel;
    }
}
