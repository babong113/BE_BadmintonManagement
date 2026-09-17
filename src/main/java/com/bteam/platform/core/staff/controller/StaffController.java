package com.bteam.platform.core.staff.controller;

import com.bteam.platform.core.auth.model.AccountStatus;
import com.bteam.platform.core.common.response.ApiResponse;
import com.bteam.platform.core.staff.dto.CreateStaffRequest;
import com.bteam.platform.core.staff.dto.OnDutyCashierResponse;
import com.bteam.platform.core.staff.dto.StaffResponse;
import com.bteam.platform.core.staff.dto.UpdateStaffRequest;
import com.bteam.platform.core.staff.service.StaffService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/staff")
@RequiredArgsConstructor
public class StaffController {
    private final StaffService staffService;

    @GetMapping
    @PreAuthorize("hasAuthority('staff:manage')")
    public ResponseEntity<ApiResponse<List<StaffResponse>>> getStaff(
            @RequestParam(defaultValue = "ACTIVE") AccountStatus status
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay danh sach nhan vien thanh cong",
                staffService.getStaff(status)
        ));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('staff:manage')")
    public ResponseEntity<ApiResponse<StaffResponse>> createStaff(
            @Valid @RequestBody CreateStaffRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Tao nhan vien thanh cong",
                staffService.createStaff(request)
        ));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<StaffResponse>> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay ho so nhan vien thanh cong",
                staffService.getMyProfile(authentication.getName())
        ));
    }

    @GetMapping("/on-duty")
    @PreAuthorize("hasAuthority('staff:manage')")
    public ResponseEntity<ApiResponse<List<OnDutyCashierResponse>>> getOnDutyCashiers(
            @RequestParam Long venueId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay danh sach thu ngan dang truc thanh cong",
                staffService.getOnDutyCashiers(venueId)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('staff:manage')")
    public ResponseEntity<ApiResponse<StaffResponse>> getStaff(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay chi tiet nhan vien thanh cong",
                staffService.getStaff(id)
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('staff:manage')")
    public ResponseEntity<ApiResponse<StaffResponse>> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cap nhat nhan vien thanh cong",
                staffService.updateStaff(id, request)
        ));
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('staff:manage')")
    public ResponseEntity<ApiResponse<StaffResponse>> lockStaff(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Khoa tai khoan nhan vien thanh cong",
                staffService.lockStaff(id)
        ));
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('staff:manage')")
    public ResponseEntity<ApiResponse<StaffResponse>> unlockStaff(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Mo lai tai khoan nhan vien thanh cong",
                staffService.unlockStaff(id)
        ));
    }
}
