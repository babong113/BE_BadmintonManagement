package com.bteam.platform.core.court.dto;

import com.bteam.platform.core.venue.model.CourtStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CourtRequest(
        @NotNull(message = "Co so khong duoc de trong")
        Long venueId,

        @NotBlank(message = "Ma san khong duoc de trong")
        String courtCode,

        @NotBlank(message = "Ten san khong duoc de trong")
        String name,

        String description,

        @NotNull(message = "Gia thue khong duoc de trong")
        @DecimalMin(value = "0.0", message = "Gia thue phai lon hon hoac bang 0")
        BigDecimal pricePerHour,

        CourtStatus status,

        String imageUrl
) {
}
