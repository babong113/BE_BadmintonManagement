package com.bteam.platform.core.cashier_shift.controller;

import com.bteam.platform.core.cashier_shift.dto.CashMovementResponse;
import com.bteam.platform.core.cashier_shift.dto.CashOutRequest;
import com.bteam.platform.core.cashier_shift.dto.CashierShiftResponse;
import com.bteam.platform.core.cashier_shift.dto.CloseCashierShiftRequest;
import com.bteam.platform.core.cashier_shift.dto.OpenCashierShiftRequest;
import com.bteam.platform.core.cashier_shift.dto.OpenShiftStatusResponse;
import com.bteam.platform.core.cashier_shift.service.CashierShiftService;
import com.bteam.platform.core.common.response.ApiResponse;
import com.bteam.platform.core.payment.model.CashierShiftStatus;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/cashier-shifts")
@RequiredArgsConstructor
public class CashierShiftController {
    private final CashierShiftService cashierShiftService;

    @PostMapping("/open")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<CashierShiftResponse>> openShift(
            Authentication authentication,
            @Valid @RequestBody OpenCashierShiftRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Mo ca thu ngan thanh cong",
                cashierShiftService.openShift(authentication.getName(), request)
        ));
    }

    @GetMapping("/current/status")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<OpenShiftStatusResponse>> getOpenStatus(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Kiem tra ca dang mo thanh cong",
                cashierShiftService.getOpenStatus(authentication.getName())
        ));
    }

    @GetMapping("/current")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<CashierShiftResponse>> getCurrentShift(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay ca thu ngan hien tai thanh cong",
                cashierShiftService.getCurrentShift(authentication.getName())
        ));
    }

    @GetMapping("/current/movements")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<List<CashMovementResponse>>> getCurrentMovements(
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay lich su dong tien trong ca thanh cong",
                cashierShiftService.getCurrentMovements(authentication.getName())
        ));
    }

    @PostMapping("/current/cash-out")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<CashMovementResponse>> recordCashOut(
            Authentication authentication,
            @Valid @RequestBody CashOutRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Ghi nhan tien ra khoi ket thanh cong",
                cashierShiftService.recordCashOut(authentication.getName(), request)
        ));
    }

    @PatchMapping("/current/close")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<CashierShiftResponse>> closeShift(
            Authentication authentication,
            @Valid @RequestBody CloseCashierShiftRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Dong ca thu ngan thanh cong",
                cashierShiftService.closeShift(authentication.getName(), request)
        ));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<List<CashierShiftResponse>>> getMyHistory(
            Authentication authentication,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay lich su ca ca nhan thanh cong",
                cashierShiftService.getMyHistory(authentication.getName(), from, to)
        ));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('staff:manage')")
    public ResponseEntity<ApiResponse<List<CashierShiftResponse>>> getAllShifts(
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) CashierShiftStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay lich su tat ca ca thu ngan thanh cong",
                cashierShiftService.getAllShifts(staffId, venueId, status, from, to)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('staff:manage')")
    public ResponseEntity<ApiResponse<CashierShiftResponse>> getShiftDetails(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay chi tiet doi soat ca thanh cong",
                cashierShiftService.getShiftDetails(id)
        ));
    }
}
