package com.bteam.platform.core.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "auth.registration")
public record AuthProperties(
        String defaultRole,
        List<String> allowedRoles
) {
    public AuthProperties {
        if (defaultRole == null || defaultRole.isBlank()) {
            defaultRole = "CUSTOMER";
        }
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            allowedRoles = List.of("CUSTOMER", "STAFF", "OWNER");
        }
    }
}
