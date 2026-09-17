package com.bteam.platform.core.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ChangeTimeRequest(
        @NotNull(message = "Gio bat dau khong duoc de trong")
        LocalTime startTime,

        @NotNull(message = "Gio ket thuc khong duoc de trong")
        LocalTime endTime
) {
}
