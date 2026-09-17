package com.bteam.platform.core.staff.service;

import com.bteam.platform.adapter.persistence.jpa.entity.RoleEntity;
import com.bteam.platform.adapter.persistence.jpa.entity.UserEntity;
import com.bteam.platform.adapter.persistence.jpa.repository.RoleRepository;
import com.bteam.platform.core.auth.model.AccountStatus;
import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.staff.dto.CreateStaffRequest;
import com.bteam.platform.core.staff.dto.OnDutyCashierResponse;
import com.bteam.platform.core.staff.dto.StaffResponse;
import com.bteam.platform.core.staff.dto.UpdateStaffRequest;
import com.bteam.platform.core.staff.model.StaffProfile;
import com.bteam.platform.core.staff.repository.StaffProfileRepository;
import com.bteam.platform.core.staff.repository.StaffUserRepository;
import com.bteam.platform.core.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StaffService {
    private final StaffProfileRepository staffProfileRepository;
    private final StaffUserRepository staffUserRepository;
    private final RoleRepository roleRepository;
    private final VenueRepository venueRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<StaffResponse> getStaff(AccountStatus status) {
        return staffProfileRepository.findStaff(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StaffResponse getStaff(Long id) {
        return toResponse(findStaff(id));
    }

    @Transactional(readOnly = true)
    public StaffResponse getMyProfile(String email) {
        return staffProfileRepository.findByUserEmailIgnoreCase(email)
                .map(this::toResponse)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay ho so nhan vien"));
    }

    @Transactional
    public StaffResponse createStaff(CreateStaffRequest request) {
        String email = normalizeEmail(request.email());
        String phoneNumber = request.phoneNumber().trim();
        String employeeCode = normalizeEmployeeCode(request.employeeCode());
        String identityCard = trimToNull(request.identityCard());
        validateCreateUniqueness(email, phoneNumber, employeeCode, identityCard);

        RoleEntity staffRole = roleRepository.findByName("STAFF")
                .orElseThrow(() -> new InvalidDataException("Khong tim thay role STAFF"));
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(phoneNumber);
        user.setFullName(request.fullName().trim());
        user.setAvatarUrl(trimToNull(request.avatarUrl()));
        user.setStatus(AccountStatus.ACTIVE);
        user.setRoles(new HashSet<>(Set.of(staffRole)));
        UserEntity savedUser = staffUserRepository.save(user);

        StaffProfile profile = StaffProfile.builder()
                .user(savedUser)
                .employeeCode(employeeCode)
                .identityCard(identityCard)
                .hireDate(request.hireDate())
                .position(normalizePosition(request.position()))
                .notes(trimToNull(request.notes()))
                .build();
        return toResponse(staffProfileRepository.save(profile));
    }

    @Transactional
    public StaffResponse updateStaff(Long id, UpdateStaffRequest request) {
        StaffProfile profile = findStaff(id);
        UserEntity user = profile.getUser();
        String email = normalizeEmail(request.email());
        String phoneNumber = request.phoneNumber().trim();
        String employeeCode = normalizeEmployeeCode(request.employeeCode());
        String identityCard = trimToNull(request.identityCard());
        validateUpdateUniqueness(id, email, phoneNumber, employeeCode, identityCard);

        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setFullName(request.fullName().trim());
        user.setAvatarUrl(trimToNull(request.avatarUrl()));
        profile.setEmployeeCode(employeeCode);
        profile.setIdentityCard(identityCard);
        profile.setHireDate(request.hireDate());
        profile.setPosition(normalizePosition(request.position()));
        profile.setNotes(trimToNull(request.notes()));

        staffUserRepository.save(user);
        return toResponse(staffProfileRepository.save(profile));
    }

    @Transactional
    public StaffResponse lockStaff(Long id) {
        StaffProfile profile = findStaff(id);
        profile.getUser().setStatus(AccountStatus.BLOCKED);
        staffUserRepository.save(profile.getUser());
        return toResponse(profile);
    }

    @Transactional
    public StaffResponse unlockStaff(Long id) {
        StaffProfile profile = findStaff(id);
        profile.getUser().setStatus(AccountStatus.ACTIVE);
        staffUserRepository.save(profile.getUser());
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public List<OnDutyCashierResponse> getOnDutyCashiers(Long venueId) {
        if (!venueRepository.existsById(venueId)) {
            throw new InvalidDataException("Khong tim thay co so");
        }
        return staffProfileRepository.findOnDutyCashiers(venueId)
                .stream()
                .map(item -> new OnDutyCashierResponse(
                        item.getStaffId(),
                        item.getEmployeeCode(),
                        item.getFullName(),
                        item.getEmail(),
                        item.getPhoneNumber(),
                        item.getPosition(),
                        item.getShiftId(),
                        item.getOpenedAt(),
                        item.getShiftStatus(),
                        item.getVenueId(),
                        item.getVenueName()
                ))
                .toList();
    }

    private StaffProfile findStaff(Long id) {
        return staffProfileRepository.findByUserId(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay nhan vien"));
    }

    private void validateCreateUniqueness(
            String email,
            String phoneNumber,
            String employeeCode,
            String identityCard
    ) {
        if (staffUserRepository.existsByEmailIgnoreCase(email)) {
            throw new InvalidDataException("Email da duoc su dung");
        }
        if (staffUserRepository.existsByPhoneNumber(phoneNumber)) {
            throw new InvalidDataException("So dien thoai da duoc su dung");
        }
        if (staffProfileRepository.existsByEmployeeCodeIgnoreCase(employeeCode)) {
            throw new InvalidDataException("Ma nhan vien da ton tai");
        }
        if (identityCard != null && staffProfileRepository.existsByIdentityCard(identityCard)) {
            throw new InvalidDataException("CCCD da duoc su dung");
        }
    }

    private void validateUpdateUniqueness(
            Long id,
            String email,
            String phoneNumber,
            String employeeCode,
            String identityCard
    ) {
        if (staffUserRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new InvalidDataException("Email da duoc su dung");
        }
        if (staffUserRepository.existsByPhoneNumberAndIdNot(phoneNumber, id)) {
            throw new InvalidDataException("So dien thoai da duoc su dung");
        }
        if (staffProfileRepository.existsByEmployeeCodeIgnoreCaseAndUserIdNot(employeeCode, id)) {
            throw new InvalidDataException("Ma nhan vien da ton tai");
        }
        if (identityCard != null && staffProfileRepository.existsByIdentityCardAndUserIdNot(identityCard, id)) {
            throw new InvalidDataException("CCCD da duoc su dung");
        }
    }

    private StaffResponse toResponse(StaffProfile profile) {
        UserEntity user = profile.getUser();
        return new StaffResponse(
                user.getId(),
                profile.getEmployeeCode(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                profile.getIdentityCard(),
                profile.getHireDate(),
                profile.getPosition(),
                user.getStatus(),
                profile.getNotes(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmployeeCode(String employeeCode) {
        return employeeCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePosition(String position) {
        return position.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
