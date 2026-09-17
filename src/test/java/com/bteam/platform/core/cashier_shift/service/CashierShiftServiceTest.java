package com.bteam.platform.core.cashier_shift.service;

import com.bteam.platform.adapter.persistence.jpa.entity.UserEntity;
import com.bteam.platform.core.cashier_shift.dto.CashOutRequest;
import com.bteam.platform.core.cashier_shift.dto.CloseCashierShiftRequest;
import com.bteam.platform.core.cashier_shift.dto.OpenCashierShiftRequest;
import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.payment.model.CashMovement;
import com.bteam.platform.core.payment.model.CashMovementType;
import com.bteam.platform.core.payment.model.CashierShift;
import com.bteam.platform.core.payment.model.CashierShiftStatus;
import com.bteam.platform.core.payment.repository.CashMovementRepository;
import com.bteam.platform.core.payment.repository.CashierShiftRepository;
import com.bteam.platform.core.staff.model.StaffProfile;
import com.bteam.platform.core.staff.repository.StaffProfileRepository;
import com.bteam.platform.core.venue.model.Venue;
import com.bteam.platform.core.venue.model.VenueStatus;
import com.bteam.platform.core.venue.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashierShiftServiceTest {
    @Mock
    private CashierShiftRepository cashierShiftRepository;
    @Mock
    private CashMovementRepository cashMovementRepository;
    @Mock
    private StaffProfileRepository staffProfileRepository;
    @Mock
    private VenueRepository venueRepository;

    private CashierShiftService cashierShiftService;
    private StaffProfile cashier;
    private Venue venue;

    @BeforeEach
    void setUp() {
        cashierShiftService = new CashierShiftService(
                cashierShiftRepository,
                cashMovementRepository,
                staffProfileRepository,
                venueRepository
        );
        UserEntity user = new UserEntity();
        user.setId(10L);
        user.setEmail("cashier@example.com");
        user.setFullName("Thu Ngan A");
        cashier = StaffProfile.builder()
                .userId(10L)
                .user(user)
                .employeeCode("NV-001")
                .position("CASHIER")
                .build();
        venue = Venue.builder()
                .id(5L)
                .name("Co so 1")
                .address("Dia chi")
                .status(VenueStatus.ACTIVE)
                .build();
    }

    @Test
    void openShiftStoresOpeningCashVenueAndOpenStatus() {
        mockCashier();
        when(cashierShiftRepository.findByStaffIdAndStatus(10L, CashierShiftStatus.OPEN))
                .thenReturn(Optional.empty());
        when(venueRepository.findByIdAndStatus(5L, VenueStatus.ACTIVE)).thenReturn(Optional.of(venue));
        when(cashierShiftRepository.saveAndFlush(any(CashierShift.class))).thenAnswer(invocation -> {
            CashierShift shift = invocation.getArgument(0);
            shift.setId(50L);
            return shift;
        });
        mockMovementSums("0.00", "0.00");

        var response = cashierShiftService.openShift(
                "cashier@example.com",
                new OpenCashierShiftRequest(5L, new BigDecimal("500000.00"), "Ca sang")
        );

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.openingCash()).isEqualByComparingTo("500000.00");
        assertThat(response.expectedClosingCash()).isEqualByComparingTo("500000.00");
        assertThat(response.status()).isEqualTo(CashierShiftStatus.OPEN);
    }

    @Test
    void employeeCannotOpenMoreThanOneShift() {
        mockCashier();
        when(cashierShiftRepository.findByStaffIdAndStatus(10L, CashierShiftStatus.OPEN))
                .thenReturn(Optional.of(openShift()));

        assertThatThrownBy(() -> cashierShiftService.openShift(
                "cashier@example.com",
                new OpenCashierShiftRequest(5L, new BigDecimal("500000.00"), null)
        ))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Nhan vien da co ca thu ngan OPEN");
    }

    @Test
    void currentShiftCalculatesExpectedCashFromCashInAndCashOut() {
        mockCashier();
        when(cashierShiftRepository.findByStaffIdAndStatus(10L, CashierShiftStatus.OPEN))
                .thenReturn(Optional.of(openShift()));
        mockMovementSums("200000.00", "50000.00");

        var response = cashierShiftService.getCurrentShift("cashier@example.com");

        assertThat(response.cashIn()).isEqualByComparingTo("200000.00");
        assertThat(response.cashOut()).isEqualByComparingTo("50000.00");
        assertThat(response.expectedClosingCash()).isEqualByComparingTo("650000.00");
    }

    @Test
    void manualCashOutCannotExceedExpectedCashInShift() {
        mockCashier();
        when(cashierShiftRepository.findByStaffIdAndStatusForUpdate(10L, CashierShiftStatus.OPEN))
                .thenReturn(Optional.of(openShift()));
        mockMovementSums("0.00", "0.00");

        assertThatThrownBy(() -> cashierShiftService.recordCashOut(
                "cashier@example.com",
                new CashOutRequest(new BigDecimal("600000.00"), "Dieu chinh")
        ))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("So tien chi vuot qua tien mat du kien trong ca");
    }

    @Test
    void closingShiftWithDifferenceRequiresReason() {
        mockCashier();
        when(cashierShiftRepository.findByStaffIdAndStatusForUpdate(10L, CashierShiftStatus.OPEN))
                .thenReturn(Optional.of(openShift()));
        mockMovementSums("200000.00", "50000.00");

        assertThatThrownBy(() -> cashierShiftService.closeShift(
                "cashier@example.com",
                new CloseCashierShiftRequest(new BigDecimal("640000.00"), null)
        ))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Phai ghi chu nguyen nhan khi tien cuoi ca co chenh lech");
    }

    @Test
    void closeShiftCalculatesDifferenceAndChangesStatus() {
        mockCashier();
        CashierShift shift = openShift();
        when(cashierShiftRepository.findByStaffIdAndStatusForUpdate(10L, CashierShiftStatus.OPEN))
                .thenReturn(Optional.of(shift));
        mockMovementSums("200000.00", "50000.00");
        when(cashierShiftRepository.save(shift)).thenReturn(shift);

        var response = cashierShiftService.closeShift(
                "cashier@example.com",
                new CloseCashierShiftRequest(new BigDecimal("640000.00"), "Thieu 10000")
        );

        assertThat(response.expectedClosingCash()).isEqualByComparingTo("650000.00");
        assertThat(response.actualClosingCash()).isEqualByComparingTo("640000.00");
        assertThat(response.cashDifference()).isEqualByComparingTo("-10000.00");
        assertThat(response.status()).isEqualTo(CashierShiftStatus.CLOSED);
        assertThat(response.closedAt()).isNotNull();
    }

    @Test
    void recordingCashOutCreatesAdjustmentMovement() {
        mockCashier();
        when(cashierShiftRepository.findByStaffIdAndStatusForUpdate(10L, CashierShiftStatus.OPEN))
                .thenReturn(Optional.of(openShift()));
        mockMovementSums("0.00", "0.00");
        when(cashMovementRepository.save(any(CashMovement.class))).thenAnswer(invocation -> {
            CashMovement movement = invocation.getArgument(0);
            movement.setId(70L);
            return movement;
        });

        var response = cashierShiftService.recordCashOut(
                "cashier@example.com",
                new CashOutRequest(new BigDecimal("100000.00"), "Rut tien mua vat tu")
        );

        assertThat(response.movementType()).isEqualTo(CashMovementType.CASH_OUT);
        assertThat(response.amount()).isEqualByComparingTo("100000.00");
        assertThat(response.createdById()).isEqualTo(10L);
    }

    private void mockCashier() {
        when(staffProfileRepository.findByUserEmailIgnoreCase("cashier@example.com"))
                .thenReturn(Optional.of(cashier));
    }

    private void mockMovementSums(String cashIn, String cashOut) {
        when(cashMovementRepository.sumAmountByShiftAndType(any(), any(CashMovementType.class)))
                .thenAnswer(invocation -> invocation.getArgument(1) == CashMovementType.CASH_IN
                        ? new BigDecimal(cashIn)
                        : new BigDecimal(cashOut));
    }

    private CashierShift openShift() {
        return CashierShift.builder()
                .id(50L)
                .staff(cashier.getUser())
                .venue(venue)
                .openedAt(ZonedDateTime.now().minusHours(1))
                .openingCash(new BigDecimal("500000.00"))
                .status(CashierShiftStatus.OPEN)
                .build();
    }
}
