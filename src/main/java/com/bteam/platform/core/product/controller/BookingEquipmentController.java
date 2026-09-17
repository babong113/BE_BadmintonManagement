package com.bteam.platform.core.product.controller;

import com.bteam.platform.core.common.response.ApiResponse;
import com.bteam.platform.core.product.dto.BookingEquipmentResponse;
import com.bteam.platform.core.product.dto.RentProductRequest;
import com.bteam.platform.core.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/booking-details/{bookingDetailId}/equipment")
@RequiredArgsConstructor
public class BookingEquipmentController {
    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('booking:create', 'booking:update')")
    public ResponseEntity<ApiResponse<List<BookingEquipmentResponse>>> getRentals(
            @PathVariable Long bookingDetailId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay danh sach dung cu thue thanh cong",
                productService.getBookingDetailRentals(bookingDetailId, authentication)
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('booking:create', 'booking:update')")
    public ResponseEntity<ApiResponse<BookingEquipmentResponse>> rent(
            @PathVariable Long bookingDetailId,
            @Valid @RequestBody RentProductRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Them dung cu thue vao booking detail thanh cong",
                productService.rentForBookingDetail(bookingDetailId, request, authentication)
        ));
    }
}
