package com.bteam.platform.core.staff.service;

import com.bteam.platform.adapter.persistence.jpa.entity.RoleEntity;
import com.bteam.platform.adapter.persistence.jpa.entity.UserEntity;
import com.bteam.platform.adapter.persistence.jpa.repository.RoleRepository;
import com.bteam.platform.core.auth.model.AccountStatus;
import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.staff.dto.CreateStaffRequest;
import com.bteam.platform.core.staff.model.StaffProfile;
import com.bteam.platform.core.staff.repository.StaffProfileRepository;
import com.bteam.platform.core.staff.repository.StaffUserRepository;
import com.bteam.platform.core.venue.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {
    @Mock
    private StaffProfileRepository staffProfileRepository;
    @Mock
    private StaffUserRepository staffUserRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private StaffService staffService;

    @BeforeEach
    void setUp() {
        staffService = new StaffService(
                staffProfileRepository,
                staffUserRepository,
                roleRepository,
                venueRepository,
                passwordEncoder
        );
    }

    @Test
    void createStaffAssignsStaffRoleAndNormalizesCodeAndPosition() {
        RoleEntity staffRole = new RoleEntity();
        staffRole.setId(2L);
        staffRole.setName("STAFF");
        when(roleRepository.findByName("STAFF")).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(staffUserRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        when(staffProfileRepository.save(any(StaffProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = staffService.createStaff(createRequest("nv-001", "cashier"));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.employeeCode()).isEqualTo("NV-001");
        assertThat(response.position()).isEqualTo("CASHIER");
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        verify(passwordEncoder).encode("123456");
    }

    @Test
    void createStaffRejectsDuplicateEmployeeCodeIgnoringCase() {
        when(staffProfileRepository.existsByEmployeeCodeIgnoreCase("NV-001")).thenReturn(true);

        assertThatThrownBy(() -> staffService.createStaff(createRequest("nv-001", "cashier")))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Ma nhan vien da ton tai");
    }

    @Test
    void lockStaffBlocksAccount() {
        StaffProfile profile = profile();
        when(staffProfileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(staffUserRepository.save(profile.getUser())).thenReturn(profile.getUser());

        var response = staffService.lockStaff(10L);

        assertThat(response.status()).isEqualTo(AccountStatus.BLOCKED);
    }

    @Test
    void staffCanReadOwnProfileByEmail() {
        StaffProfile profile = profile();
        when(staffProfileRepository.findByUserEmailIgnoreCase("staff@example.com"))
                .thenReturn(Optional.of(profile));

        var response = staffService.getMyProfile("staff@example.com");

        assertThat(response.employeeCode()).isEqualTo("NV-001");
        assertThat(response.email()).isEqualTo("staff@example.com");
    }

    @Test
    void onDutyCashiersRequireExistingVenueAndMapShiftInformation() {
        StaffProfileRepository.OnDutyCashierProjection projection =
                org.mockito.Mockito.mock(StaffProfileRepository.OnDutyCashierProjection.class);
        when(venueRepository.existsById(5L)).thenReturn(true);
        when(projection.getStaffId()).thenReturn(10L);
        when(projection.getEmployeeCode()).thenReturn("NV-001");
        when(projection.getFullName()).thenReturn("Nhan Vien A");
        when(projection.getEmail()).thenReturn("staff@example.com");
        when(projection.getPhoneNumber()).thenReturn("0901234567");
        when(projection.getPosition()).thenReturn("CASHIER");
        when(projection.getShiftId()).thenReturn(50L);
        when(projection.getOpenedAt()).thenReturn(OffsetDateTime.now());
        when(projection.getShiftStatus()).thenReturn("OPEN");
        when(projection.getVenueId()).thenReturn(5L);
        when(projection.getVenueName()).thenReturn("Co so 1");
        when(staffProfileRepository.findOnDutyCashiers(5L)).thenReturn(java.util.List.of(projection));

        var result = staffService.getOnDutyCashiers(5L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().shiftId()).isEqualTo(50L);
        assertThat(result.getFirst().position()).isEqualTo("CASHIER");
        assertThat(result.getFirst().shiftStatus()).isEqualTo("OPEN");
    }

    private CreateStaffRequest createRequest(String employeeCode, String position) {
        return new CreateStaffRequest(
                "Staff@Example.com",
                "123456",
                "0901234567",
                "Nhan Vien A",
                null,
                employeeCode,
                "012345678901",
                LocalDate.now(),
                position,
                "Nhan vien test"
        );
    }

    private StaffProfile profile() {
        UserEntity user = new UserEntity();
        user.setId(10L);
        user.setEmail("staff@example.com");
        user.setPhoneNumber("0901234567");
        user.setFullName("Nhan Vien A");
        user.setStatus(AccountStatus.ACTIVE);
        return StaffProfile.builder()
                .userId(10L)
                .user(user)
                .employeeCode("NV-001")
                .identityCard("012345678901")
                .hireDate(LocalDate.now())
                .position("CASHIER")
                .build();
    }
}
