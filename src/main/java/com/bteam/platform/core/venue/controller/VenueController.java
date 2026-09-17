package com.bteam.platform.core.venue.controller;

import com.bteam.platform.core.common.response.ApiResponse;
import com.bteam.platform.core.venue.dto.CourtResponse;
import com.bteam.platform.core.venue.dto.NearbyVenueResponse;
import com.bteam.platform.core.venue.dto.VenueLocationResponse;
import com.bteam.platform.core.venue.dto.VenueRequest;
import com.bteam.platform.core.venue.dto.VenueResponse;
import com.bteam.platform.core.venue.service.VenueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
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
import java.util.List;

@Validated
@RestController
@RequestMapping("api/venues")
@RequiredArgsConstructor
public class VenueController {
    private final VenueService venueService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getActiveVenues() {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay danh sach co so thanh cong",
                venueService.getActiveVenues()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VenueResponse>> getVenueDetail(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay chi tiet co so thanh cong",
                venueService.getVenueDetail(id)
        ));
    }

    @GetMapping("/{id}/location")
    public ResponseEntity<ApiResponse<VenueLocationResponse>> getVenueLocation(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay vi tri co so thanh cong",
                venueService.getVenueLocation(id)
        ));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NearbyVenueResponse>>> getNearbyVenues(
            @RequestParam
            @DecimalMin(value = "-90.0", message = "Latitude phai lon hon hoac bang -90")
            @DecimalMax(value = "90.0", message = "Latitude phai nho hon hoac bang 90")
            BigDecimal latitude,

            @RequestParam
            @DecimalMin(value = "-180.0", message = "Longitude phai lon hon hoac bang -180")
            @DecimalMax(value = "180.0", message = "Longitude phai nho hon hoac bang 180")
            BigDecimal longitude,

            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay danh sach co so gan ban thanh cong",
                venueService.getNearbyVenues(latitude, longitude, limit)
        ));
    }

    @GetMapping("/{id}/courts")
    public ResponseEntity<ApiResponse<List<CourtResponse>>> getCourtsByVenue(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay danh sach san thuoc co so thanh cong",
                venueService.getCourtsByVenue(id)
        ));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('venue:write')")
    public ResponseEntity<ApiResponse<VenueResponse>> createVenue(@Valid @RequestBody VenueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Tao co so thanh cong",
                        venueService.createVenue(request)
                ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('venue:write')")
    public ResponseEntity<ApiResponse<VenueResponse>> updateVenue(
            @PathVariable Long id,
            @Valid @RequestBody VenueRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cap nhat co so thanh cong",
                venueService.updateVenue(id, request)
        ));
    }

    @PatchMapping("/{id}/inactive")
    @PreAuthorize("hasAuthority('venue:write')")
    public ResponseEntity<ApiResponse<VenueResponse>> deactivateVenue(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Ngung hoat dong co so thanh cong",
                venueService.deactivateVenue(id)
        ));
    }
}
