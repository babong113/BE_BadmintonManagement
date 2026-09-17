package com.bteam.platform.core.booking.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeCourtRequest(
        @NotNull(message = "San moi khong duoc de trong")
        Long courtId
) {
}
