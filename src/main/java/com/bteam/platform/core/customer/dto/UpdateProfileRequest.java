package com.bteam.platform.core.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank(message = "Ho ten khong duoc de trong")
        String fullName,

        @NotBlank(message = "So dien thoai khong duoc de trong")
        String phoneNumber,

        String avatarUrl
) {
}
