package com.bteam.platform.core.customer.controller;

import com.bteam.platform.core.common.response.ApiResponse;
import com.bteam.platform.core.customer.dto.BookingHistoryResponse;
import com.bteam.platform.core.customer.dto.ChangePasswordRequest;
import com.bteam.platform.core.customer.dto.CustomerProfileResponse;
import com.bteam.platform.core.customer.dto.CustomerSearchResponse;
import com.bteam.platform.core.customer.dto.OrderHistoryResponse;
import com.bteam.platform.core.customer.dto.PaymentHistoryResponse;
import com.bteam.platform.core.customer.dto.UpdateProfileRequest;
import com.bteam.platform.core.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay ho so ca nhan thanh cong",
                customerService.getMyProfile(authentication.getName())
        ));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cap nhat ho so thanh cong",
                customerService.updateMyProfile(authentication.getName(), request)
        ));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        customerService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doi mat khau thanh cong", null));
    }

    @GetMapping("/me/bookings")
    public ResponseEntity<ApiResponse<List<BookingHistoryResponse>>> getMyBookingHistory(
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay lich su dat san thanh cong",
                customerService.getMyBookingHistory(authentication.getName())
        ));
    }

    @GetMapping("/me/payments")
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> getMyPaymentHistory(
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay lich su thanh toan thanh cong",
                customerService.getMyPaymentHistory(authentication.getName())
        ));
    }

    @GetMapping("/me/orders")
    public ResponseEntity<ApiResponse<List<OrderHistoryResponse>>> getMyOrderHistory(
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay lich su mua hang thanh cong",
                customerService.getMyOrderHistory(authentication.getName())
        ));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('customer:read')")
    public ResponseEntity<ApiResponse<List<CustomerSearchResponse>>> searchCustomers(
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Tra cuu khach hang thanh cong",
                customerService.searchCustomers(keyword)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:read')")
    public ResponseEntity<ApiResponse<CustomerSearchResponse>> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay thong tin khach hang thanh cong",
                customerService.getCustomer(id)
        ));
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<CustomerSearchResponse>> lockCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Khoa tai khoan thanh cong",
                customerService.lockCustomer(id)
        ));
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<CustomerSearchResponse>> unlockCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Mo khoa tai khoan thanh cong",
                customerService.unlockCustomer(id)
        ));
    }
}
