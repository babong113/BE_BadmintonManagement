package com.bteam.platform.core.payment.controller;

import com.bteam.platform.core.common.response.ApiResponse;
import com.bteam.platform.core.payment.dto.AddAllocationsRequest;
import com.bteam.platform.core.payment.dto.CreatePaymentRequest;
import com.bteam.platform.core.payment.dto.PaymentResponse;
import com.bteam.platform.core.payment.dto.PaymentSummaryResponse;
import com.bteam.platform.core.payment.dto.UpdatePaymentStatusRequest;
import com.bteam.platform.core.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/bookings/{bookingId}/summary")
    @PreAuthorize("hasAnyAuthority('payment:read', 'payment:write')")
    public ResponseEntity<ApiResponse<PaymentSummaryResponse>> getBookingSummary(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay tong quan thanh toan thanh cong",
                paymentService.getBookingSummary(bookingId, authentication)
        ));
    }

    @GetMapping("/bookings/{bookingId}")
    @PreAuthorize("hasAnyAuthority('payment:read', 'payment:write')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getBookingPayments(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay lich su thanh toan thanh cong",
                paymentService.getBookingPayments(bookingId, authentication)
        ));
    }

    @GetMapping("/orders/{orderId}/summary")
    @PreAuthorize("hasAnyAuthority('payment:read', 'payment:write')")
    public ResponseEntity<ApiResponse<PaymentSummaryResponse>> getOrderSummary(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay tong quan thanh toan order thanh cong",
                paymentService.getOrderSummary(orderId, authentication)
        ));
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyAuthority('payment:read', 'payment:write')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getOrderPayments(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay lich su thanh toan order thanh cong",
                paymentService.getOrderPayments(orderId, authentication)
        ));
    }

    @PostMapping("/bookings/{bookingId}")
    @PreAuthorize("hasAnyAuthority('payment:read', 'payment:write')")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @PathVariable Long bookingId,
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Tao payment thanh cong",
                paymentService.createPayment(bookingId, request, authentication)
        ));
    }

    @PostMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyAuthority('payment:read', 'payment:write')")
    public ResponseEntity<ApiResponse<PaymentResponse>> createOrderPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Tao payment cho order thanh cong",
                paymentService.createOrderPayment(orderId, request, authentication)
        ));
    }

    @PatchMapping("/{paymentId}/status")
    @PreAuthorize("hasAuthority('payment:write')")
    public ResponseEntity<ApiResponse<PaymentResponse>> updateStatus(
            @PathVariable Long paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cap nhat trang thai payment thanh cong",
                paymentService.updateStatus(paymentId, request, authentication)
        ));
    }

    @PostMapping("/{paymentId}/allocations")
    @PreAuthorize("hasAuthority('payment:write')")
    public ResponseEntity<ApiResponse<PaymentResponse>> addAllocations(
            @PathVariable Long paymentId,
            @Valid @RequestBody AddAllocationsRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Phan bo payment thanh cong",
                paymentService.addAllocations(paymentId, request)
        ));
    }

    @PatchMapping("/{paymentId}/refund")
    @PreAuthorize("hasAuthority('payment:write')")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @PathVariable Long paymentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Hoan tien payment thanh cong",
                paymentService.refund(paymentId, authentication)
        ));
    }
}
