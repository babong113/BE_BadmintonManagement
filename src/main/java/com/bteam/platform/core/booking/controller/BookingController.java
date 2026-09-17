package com.bteam.platform.core.booking.controller;

import com.bteam.platform.core.booking.dto.AvailableCourtResponse;
import com.bteam.platform.core.booking.dto.BookingResponse;
import com.bteam.platform.core.booking.dto.BookingVenueResponse;
import com.bteam.platform.core.booking.dto.ChangeBookingVenueRequest;
import com.bteam.platform.core.booking.dto.ChangeCourtRequest;
import com.bteam.platform.core.booking.dto.ChangeTimeRequest;
import com.bteam.platform.core.booking.dto.CreateBookingRequest;
import com.bteam.platform.core.booking.dto.UpdateBookingRequest;
import com.bteam.platform.core.booking.service.BookingService;
import com.bteam.platform.core.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @GetMapping("/available-courts")
    @PreAuthorize("hasAnyAuthority('booking:create', 'booking:read')")
    public ResponseEntity<ApiResponse<List<AvailableCourtResponse>>> findAvailableCourts(
            @RequestParam(required = false) Long venueId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate bookingDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime startTime,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime endTime
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Tim san trong thanh cong",
                bookingService.findAvailableCourts(venueId, bookingDate, startTime, endTime)
        ));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('booking:create')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Tao booking thanh cong",
                        bookingService.createBooking(request, authentication)
                ));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('booking:update')")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Xac nhan booking thanh cong",
                bookingService.confirmBooking(id, authentication)
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay booking ca nhan thanh cong",
                bookingService.getMyBookings(authentication)
        ));
    }

    @GetMapping("/schedule")
    @PreAuthorize("hasAuthority('booking:read')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getSchedule(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate bookingDate,

            @RequestParam(required = false) Long courtId,
            @RequestParam(required = false) Long venueId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay lich dat san thanh cong",
                bookingService.getSchedule(bookingDate, courtId, venueId)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingDetail(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay chi tiet booking thanh cong",
                bookingService.getBookingDetail(id, authentication)
        ));
    }

    @GetMapping("/{id}/venue")
    public ResponseEntity<ApiResponse<BookingVenueResponse>> getBookingVenue(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay co so cua booking thanh cong",
                bookingService.getBookingVenue(id, authentication)
        ));
    }

    @PatchMapping("/{id}/venue")
    @PreAuthorize("hasAnyAuthority('booking:create', 'booking:update')")
    public ResponseEntity<ApiResponse<BookingVenueResponse>> changeBookingVenue(
            @PathVariable Long id,
            @Valid @RequestBody ChangeBookingVenueRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cap nhat co so cua booking thanh cong",
                bookingService.changeBookingVenue(id, request, authentication)
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('booking:update')")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(
            @PathVariable Long id,
            @RequestBody UpdateBookingRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cap nhat booking thanh cong",
                bookingService.updateBooking(id, request)
        ));
    }

    @PatchMapping("/details/{detailId}/court")
    @PreAuthorize("hasAuthority('booking:update')")
    public ResponseEntity<ApiResponse<BookingResponse>> changeCourt(
            @PathVariable Long detailId,
            @Valid @RequestBody ChangeCourtRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Doi san thanh cong",
                bookingService.changeCourt(detailId, request)
        ));
    }

    @PatchMapping("/details/{detailId}/time")
    @PreAuthorize("hasAuthority('booking:update')")
    public ResponseEntity<ApiResponse<BookingResponse>> changeTime(
            @PathVariable Long detailId,
            @Valid @RequestBody ChangeTimeRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Doi gio dat san thanh cong",
                bookingService.changeTime(detailId, request)
        ));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('booking:cancel')")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Huy booking thanh cong",
                bookingService.cancelBooking(id, authentication)
        ));
    }

    @PatchMapping("/details/{detailId}/cancel")
    @PreAuthorize("hasAuthority('booking:update')")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBookingDetail(@PathVariable Long detailId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Huy san trong booking thanh cong",
                bookingService.cancelBookingDetail(detailId)
        ));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('booking:update')")
    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Hoan tat booking thanh cong",
                bookingService.completeBooking(id)
        ));
    }
}
