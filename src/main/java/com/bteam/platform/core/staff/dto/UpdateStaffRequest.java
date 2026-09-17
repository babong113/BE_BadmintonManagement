package com.bteam.platform.core.staff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateStaffRequest(
        @NotBlank(message = "Email khong duoc de trong")
        @Email(message = "Email khong hop le")
        String email,

        @NotBlank(message = "So dien thoai khong duoc de trong")
        @Pattern(regexp = "^[0-9]{10,11}$", message = "So dien thoai khong hop le")
        String phoneNumber,

        @NotBlank(message = "Ho ten khong duoc de trong")
        @Size(max = 255, message = "Ho ten khong duoc vuot qua 255 ky tu")
        String fullName,

        String avatarUrl,

        @NotBlank(message = "Ma nhan vien khong duoc de trong")
        @Size(max = 50, message = "Ma nhan vien khong duoc vuot qua 50 ky tu")
        String employeeCode,

        @Size(max = 20, message = "CCCD khong duoc vuot qua 20 ky tu")
        String identityCard,

        @PastOrPresent(message = "Ngay vao lam khong duoc o tuong lai")
        LocalDate hireDate,

        @NotBlank(message = "Vi tri nhan vien khong duoc de trong")
        @Size(max = 100, message = "Vi tri khong duoc vuot qua 100 ky tu")
        String position,

        @Size(max = 2000, message = "Ghi chu khong duoc vuot qua 2000 ky tu")
        String notes
) {
}
