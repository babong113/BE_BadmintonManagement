package com.bteam.platform.core.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateBookingRequest(
        @NotNull(message = "Co so khong duoc de trong")
        Long venueId,

        Long customerId,
        String guestName,
        String guestPhone,

        @NotNull(message = "Ngay dat san khong duoc de trong")
        LocalDate bookingDate,

        String note,

        @Valid
        @NotEmpty(message = "Booking phai co it nhat mot san")
        List<BookingDetailRequest> details
) {
}
