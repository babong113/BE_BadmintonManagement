package com.bteam.platform.core.venue.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalTime;

public record VenueRequest(
        @NotBlank(message = "Ten co so khong duoc de trong")
        String name,

        @NotBlank(message = "Dia chi khong duoc de trong")
        String address,

        String phoneNumber,

        @DecimalMin(value = "-90.0", message = "Latitude phai lon hon hoac bang -90")
        @DecimalMax(value = "90.0", message = "Latitude phai nho hon hoac bang 90")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "Longitude phai lon hon hoac bang -180")
        @DecimalMax(value = "180.0", message = "Longitude phai nho hon hoac bang 180")
        BigDecimal longitude,

        LocalTime openingTime,

        LocalTime closingTime
) {
}
