package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.entity.BusinessUnitEntity;
import com.rassini.employeeportal.entity.MenuEntity;
import com.rassini.employeeportal.entity.MenuParameterEntity;
import com.rassini.employeeportal.entity.TargetType;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.service.MenuUrlResolverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MenuUrlResolverServiceImpl implements MenuUrlResolverService {

    private static final Set<String> FORBIDDEN_TOKENS = Set.of(
            "${JWT}",
            "${TOKEN}",
            "${PASSWORD}",
            "${SECRET}",
            "${APIKEY}",
            "${API_KEY}"
    );

    private static final Set<String> FORBIDDEN_NAMES = Set.of(
            "JWT",
            "TOKEN",
            "PASSWORD",
            "SECRET",
            "APIKEY",
            "API_KEY",
            "BEARER"
    );

    private static final java.util.List<com.rassini.employeeportal.dto.response.PlaceholderResponse> SUPPORTED_PLACEHOLDERS = java.util.List.of(
            new com.rassini.employeeportal.dto.response.PlaceholderResponse("BUSINESS_UNIT", "${BUSINESS_UNIT}", "Código de la primera Unidad de Negocio asignada (ej. 1850)"),
            new com.rassini.employeeportal.dto.response.PlaceholderResponse("BUSINESS_UNITS", "${BUSINESS_UNITS}", "Códigos de todas las Unidades de Negocio asignadas separados por comas (ej. 0111,1850)"),
            new com.rassini.employeeportal.dto.response.PlaceholderResponse("USERNAME", "${USERNAME}", "Nombre de usuario corporativo (ej. amoralesg)"),
            new com.rassini.employeeportal.dto.response.PlaceholderResponse("EMAIL", "${EMAIL}", "Correo electrónico institucional del colaborador"),
            new com.rassini.employeeportal.dto.response.PlaceholderResponse("EMPLOYEE_ID", "${EMPLOYEE_ID}", "Identificador o número de empleado en el IAM"),
            new com.rassini.employeeportal.dto.response.PlaceholderResponse("LANGUAGE", "${LANGUAGE}", "Idioma preferido de navegación (ej. es)"),
            new com.rassini.employeeportal.dto.response.PlaceholderResponse("THEME", "${THEME}", "Tema visual configurado en el portal (ej. light)")
    );

    @Override
    public java.util.List<com.rassini.employeeportal.dto.response.PlaceholderResponse> getAvailablePlaceholders() {
        return SUPPORTED_PLACEHOLDERS;
    }

    @Override
    public void validateParameterSafety(String paramName, String paramValue) {
        if (paramName != null) {
            String upperName = paramName.trim().toUpperCase(Locale.ROOT);
            if (FORBIDDEN_NAMES.contains(upperName)) {
                throw new BusinessException("El parámetro '" + paramName + "' contiene credenciales o tokens y está estrictamente prohibido por política de seguridad SSO IAM.");
            }
        }

        if (paramValue != null) {
            String upperVal = paramValue.trim().toUpperCase(Locale.ROOT);
            for (String forbidden : FORBIDDEN_TOKENS) {
                if (upperVal.contains(forbidden)) {
                    throw new BusinessException("El valor del parámetro contiene la variable prohibida '" + forbidden + "'. No se permite el transporte de credenciales ni tokens mediante query string.");
                }
            }
        }
    }

    @Override
    public String resolveUrl(MenuEntity menu, UserEntity currentUser) {
        if (menu.getTargetType() != TargetType.EXTERNO || menu.getExternalUrl() == null || menu.getExternalUrl().isBlank()) {
            return null;
        }

        String baseUrl = menu.getExternalUrl().trim();
        if (menu.getParameters() == null || menu.getParameters().isEmpty()) {
            return baseUrl;
        }

        StringBuilder queryString = new StringBuilder();
        boolean hasExistingQueryParams = baseUrl.contains("?");

        for (MenuParameterEntity param : menu.getParameters()) {
            if (param.getActive() == null || !param.getActive()) {
                continue;
            }

            // Validar seguridad
            validateParameterSafety(param.getParamName(), param.getParamValue());

            String resolvedValue = resolveVariable(param.getParamValue(), currentUser);
            if (resolvedValue == null) {
                resolvedValue = "";
            }

            if (queryString.length() > 0) {
                queryString.append("&");
            } else if (!hasExistingQueryParams) {
                queryString.append("?");
            } else {
                queryString.append("&");
            }

            queryString.append(URLEncoder.encode(param.getParamName(), StandardCharsets.UTF_8))
                       .append("=")
                       .append(URLEncoder.encode(resolvedValue, StandardCharsets.UTF_8));
        }

        return baseUrl + queryString.toString();
    }

    private String resolveVariable(String paramValue, UserEntity user) {
        if (paramValue == null) {
            return "";
        }

        String val = paramValue.trim();

        if (val.equalsIgnoreCase("${USERNAME}")) {
            return user != null ? user.getUsername() : "";
        }
        if (val.equalsIgnoreCase("${EMAIL}")) {
            return user != null ? user.getEmail() : "";
        }
        if (val.equalsIgnoreCase("${EMPLOYEE_ID}")) {
            return user != null && user.getId() != null ? user.getId().toString() : "";
        }
        if (val.equalsIgnoreCase("${BUSINESS_UNIT}")) {
            if (user != null && user.getBusinessUnits() != null && !user.getBusinessUnits().isEmpty()) {
                return user.getBusinessUnits().iterator().next().getCode();
            }
            return "";
        }
        if (val.equalsIgnoreCase("${BUSINESS_UNITS}")) {
            if (user != null && user.getBusinessUnits() != null && !user.getBusinessUnits().isEmpty()) {
                return user.getBusinessUnits()
                        .stream()
                        .map(BusinessUnitEntity::getCode)
                        .sorted()
                        .collect(Collectors.joining(","));
            }
            return "";
        }
        if (val.equalsIgnoreCase("${LANGUAGE}")) {
            return "es";
        }
        if (val.equalsIgnoreCase("${THEME}")) {
            return "light";
        }

        return val;
    }
}
