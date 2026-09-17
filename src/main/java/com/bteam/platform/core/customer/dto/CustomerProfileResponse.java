package com.bteam.platform.core.customer.dto;

import com.bteam.platform.core.auth.model.AccountStatus;

import java.util.Set;

public record CustomerProfileResponse(
        Long id,
        String email,
        String phoneNumber,
        String fullName,
        String avatarUrl,
        AccountStatus status,
        Set<String> roles,
        Set<String> permissions
) {
}
