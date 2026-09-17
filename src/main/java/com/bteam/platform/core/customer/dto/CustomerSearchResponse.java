package com.bteam.platform.core.customer.dto;

import com.bteam.platform.core.auth.model.AccountStatus;

public record CustomerSearchResponse(
        Long id,
        String email,
        String phoneNumber,
        String fullName,
        String avatarUrl,
        AccountStatus status
) {
}
