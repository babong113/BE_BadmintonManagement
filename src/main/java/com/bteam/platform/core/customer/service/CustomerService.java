package com.bteam.platform.core.customer.service;

import com.bteam.platform.adapter.persistence.jpa.entity.PermissionEntity;
import com.bteam.platform.adapter.persistence.jpa.entity.RoleEntity;
import com.bteam.platform.adapter.persistence.jpa.entity.UserEntity;
import com.bteam.platform.core.auth.model.AccountStatus;
import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.customer.dto.BookingHistoryResponse;
import com.bteam.platform.core.customer.dto.ChangePasswordRequest;
import com.bteam.platform.core.customer.dto.CustomerProfileResponse;
import com.bteam.platform.core.customer.dto.CustomerSearchResponse;
import com.bteam.platform.core.customer.dto.OrderHistoryResponse;
import com.bteam.platform.core.customer.dto.PaymentHistoryResponse;
import com.bteam.platform.core.customer.dto.UpdateProfileRequest;
import com.bteam.platform.core.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public CustomerProfileResponse getMyProfile(String email) {
        return toProfileResponse(findByEmail(email));
    }

    @Transactional
    public CustomerProfileResponse updateMyProfile(String email, UpdateProfileRequest request) {
        UserEntity user = findByEmail(email);
        String phoneNumber = request.phoneNumber().trim();

        if (customerRepository.existsByPhoneNumberAndIdNot(phoneNumber, user.getId())) {
            throw new InvalidDataException("So dien thoai da duoc su dung");
        }

        user.setFullName(request.fullName().trim());
        user.setPhoneNumber(phoneNumber);
        user.setAvatarUrl(trimToNull(request.avatarUrl()));

        return toProfileResponse(customerRepository.save(user));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        UserEntity user = findByEmail(email);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidDataException("Mat khau hien tai khong dung");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new InvalidDataException("Xac nhan mat khau khong khop");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new InvalidDataException("Mat khau moi khong duoc trung mat khau hien tai");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        customerRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<BookingHistoryResponse> getMyBookingHistory(String email) {
        Long customerId = findByEmail(email).getId();
        return customerRepository.findBookingHistory(customerId)
                .stream()
                .map(booking -> new BookingHistoryResponse(
                        booking.getId(),
                        booking.getBookingCode(),
                        booking.getBookingDate(),
                        booking.getTotalAmount(),
                        booking.getStatus(),
                        booking.getNote(),
                        booking.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getMyPaymentHistory(String email) {
        Long customerId = findByEmail(email).getId();
        return customerRepository.findPaymentHistory(customerId)
                .stream()
                .map(payment -> new PaymentHistoryResponse(
                        payment.getId(),
                        payment.getBookingId(),
                        payment.getBookingCode(),
                        payment.getAmount(),
                        payment.getPaymentMethod(),
                        payment.getPaymentStatus(),
                        payment.getTransactionCode(),
                        payment.getPaidAt(),
                        payment.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryResponse> getMyOrderHistory(String email) {
        Long customerId = findByEmail(email).getId();
        return customerRepository.findOrderHistory(customerId)
                .stream()
                .map(order -> new OrderHistoryResponse(
                        order.getId(),
                        order.getOrderCode(),
                        order.getTotalAmount(),
                        order.getStatus(),
                        order.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerSearchResponse> searchCustomers(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() < 2) {
            throw new InvalidDataException("Tu khoa tim kiem phai co it nhat 2 ky tu");
        }

        return customerRepository.searchCustomers(normalizedKeyword)
                .stream()
                .map(this::toCustomerSearchResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerSearchResponse getCustomer(Long id) {
        return customerRepository.findCustomerById(id)
                .map(this::toCustomerSearchResponse)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay khach hang"));
    }

    @Transactional
    public CustomerSearchResponse lockCustomer(Long id) {
        UserEntity customer = customerRepository.findCustomerById(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay khach hang"));

        customer.setStatus(AccountStatus.BLOCKED);
        return toCustomerSearchResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerSearchResponse unlockCustomer(Long id) {
        UserEntity customer = customerRepository.findCustomerById(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay khach hang"));

        customer.setStatus(AccountStatus.ACTIVE);
        return toCustomerSearchResponse(customerRepository.save(customer));
    }

    private UserEntity findByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidDataException("Nguoi dung khong ton tai"));
    }

    private CustomerProfileResponse toProfileResponse(UserEntity user) {
        Set<String> roles = user.getRoles()
                .stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());
        Set<String> permissions = user.getRoles()
                .stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());

        return new CustomerProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getStatus(),
                roles,
                permissions
        );
    }

    private CustomerSearchResponse toCustomerSearchResponse(UserEntity user) {
        return new CustomerSearchResponse(
                user.getId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getStatus()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
