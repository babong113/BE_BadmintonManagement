package com.bteam.platform.core.payment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddAllocationsRequest(
        @NotEmpty(message = "Danh sach phan bo khong duoc de trong")
        List<@Valid AllocationRequest> allocations
) {
}
