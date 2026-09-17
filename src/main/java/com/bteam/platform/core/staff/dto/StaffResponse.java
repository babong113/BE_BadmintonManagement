package com.bteam.platform.core.staff.dto;

import com.bteam.platform.core.auth.model.AccountStatus;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public record StaffResponse(
        Long id,
        String employeeCode,
        String fullName,
        String email,
        String phoneNumber,
        String avatarUrl,
        String identityCard,
        LocalDate hireDate,
        String position,
        AccountStatus status,
        String notes,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {
}
