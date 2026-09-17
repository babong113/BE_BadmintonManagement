package com.bteam.platform.core.court.controller;

import com.bteam.platform.core.common.response.ApiResponse;
import com.bteam.platform.core.court.dto.CourtAvailabilityResponse;
import com.bteam.platform.core.court.dto.CourtRequest;
import com.bteam.platform.core.court.dto.CourtResponse;
import com.bteam.platform.core.court.dto.CourtStatusResponse;
import com.bteam.platform.core.court.service.CourtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("api/courts")
@RequiredArgsConstructor
public class CourtController {
    private final CourtService courtService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourtResponse>>> getCourts() {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay danh sach san thanh cong",
                courtService.getCourts()
        ));
    }

    @GetMapping("/venue/{venueId}")
    public ResponseEntity<ApiResponse<List<CourtResponse>>> getCourtsByVenue(@PathVariable Long venueId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay danh sach san theo co so thanh cong",
                courtService.getCourtsByVenue(venueId)
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CourtResponse>>> searchCourts(@RequestParam String keyword) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Tim kiem san thanh cong",
                courtService.searchCourts(keyword)
        ));
    }

    @GetMapping("/price-range")
    public ResponseEntity<ApiResponse<List<CourtResponse>>> filterByPrice(
            @RequestParam(required = false) @DecimalMin(value = "0.0", message = "Gia toi thieu phai >= 0")
            BigDecimal minPrice,

            @RequestParam(required = false) @DecimalMin(value = "0.0", message = "Gia toi da phai >= 0")
            BigDecimal maxPrice
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Loc san theo gia thanh cong",
                courtService.filterByPrice(minPrice, maxPrice)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourtResponse>> getCourtDetail(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay chi tiet san thanh cong",
                courtService.getCourtDetail(id)
        ));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CourtStatusResponse>> getCourtStatus(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay trang thai san thanh cong",
                courtService.getCourtStatus(id)
        ));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<CourtAvailabilityResponse>> checkAvailability(
            @PathVariable Long id,

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
                "Kiem tra san trong thanh cong",
                courtService.checkAvailability(id, bookingDate, startTime, endTime)
        ));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('court:write')")
    public ResponseEntity<ApiResponse<CourtResponse>> createCourt(@Valid @RequestBody CourtRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Tao san thanh cong",
                        courtService.createCourt(request)
                ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('court:write')")
    public ResponseEntity<ApiResponse<CourtResponse>> updateCourt(
            @PathVariable Long id,
            @Valid @RequestBody CourtRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cap nhat san thanh cong",
                courtService.updateCourt(id, request)
        ));
    }

    @PatchMapping("/{id}/inactive")
    @PreAuthorize("hasAuthority('court:write')")
    public ResponseEntity<ApiResponse<CourtResponse>> deactivateCourt(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Ngung hoat dong san thanh cong",
                courtService.deactivateCourt(id)
        ));
    }

    @PatchMapping("/{id}/maintenance")
    @PreAuthorize("hasAuthority('court:write')")
    public ResponseEntity<ApiResponse<CourtResponse>> markMaintenance(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Dua san vao bao tri thanh cong",
                courtService.markMaintenance(id)
        ));
    }

    @PatchMapping("/{id}/available")
    @PreAuthorize("hasAuthority('court:write')")
    public ResponseEntity<ApiResponse<CourtResponse>> reopenCourt(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Mo lai san thanh cong",
                courtService.reopenCourt(id)
        ));
    }
}
