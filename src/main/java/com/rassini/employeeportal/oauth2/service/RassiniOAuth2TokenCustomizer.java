package com.rassini.employeeportal.oauth2.service;

import com.rassini.employeeportal.dto.response.BusinessUnitResponse;
import com.rassini.employeeportal.dto.response.UserAccessContextResponse;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.AccessContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RassiniOAuth2TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final UserRepository userRepository;
    private final AccessContextService accessContextService;

    @Override
    public void customize(JwtEncodingContext context) {
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType()) ||
            "id_token".equals(context.getTokenType().getValue())) {

            Authentication principal = context.getPrincipal();
            if (principal == null || principal.getName() == null) {
                return;
            }

            String username = principal.getName();
            Optional<UserEntity> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                log.warn("User {} not found while customizing OAuth2 token", username);
                return;
            }

            UserEntity user = userOpt.get();
            UserAccessContextResponse accessContext = accessContextService.getAccessContext(user.getId());

            List<Map<String, Object>> buList = new ArrayList<>();
            List<String> buCodes = new ArrayList<>();
            if (accessContext.getBusinessUnits() != null) {
                for (BusinessUnitResponse bu : accessContext.getBusinessUnits()) {
                    Map<String, Object> buMap = new LinkedHashMap<>();
                    buMap.put("id", bu.getId());
                    buMap.put("code", bu.getCode());
                    buMap.put("name", bu.getName());
                    buList.add(buMap);
                    buCodes.add(bu.getCode());
                }
            }

            context.getClaims().claims(claims -> {
                claims.put("userId", user.getId());
                claims.put("employeeId", String.valueOf(user.getId()));
                claims.put("email", user.getEmail());
                claims.put("roles", accessContext.getRoles() != null ? accessContext.getRoles() : List.of());
                claims.put("permissions", accessContext.getPermissions() != null ? accessContext.getPermissions() : List.of());
                claims.put("businessUnits", buList);
                claims.put("hasAllBusinessUnits", Boolean.TRUE.equals(accessContext.getHasAllBusinessUnits()));
                claims.put("bu", String.join(",", buCodes));
            });

            log.debug("Customized OAuth2 {} with claims for user {}", context.getTokenType().getValue(), username);
        }
    }
}
