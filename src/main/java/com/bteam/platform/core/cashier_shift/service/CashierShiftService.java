package com.bteam.platform.core.cashier_shift.service;

import com.bteam.platform.adapter.persistence.jpa.entity.UserEntity;
import com.bteam.platform.core.cashier_shift.dto.CashMovementResponse;
import com.bteam.platform.core.cashier_shift.dto.CashOutRequest;
import com.bteam.platform.core.cashier_shift.dto.CashierShiftResponse;
import com.bteam.platform.core.cashier_shift.dto.CloseCashierShiftRequest;
import com.bteam.platform.core.cashier_shift.dto.OpenCashierShiftRequest;
import com.bteam.platform.core.cashier_shift.dto.OpenShiftStatusResponse;
import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.payment.model.CashMovement;
import com.bteam.platform.core.payment.model.CashMovementSourceType;
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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashierShiftService {
    private final CashierShiftRepository cashierShiftRepository;
    private final CashMovementRepository cashMovementRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final VenueRepository venueRepository;

    @Transactional
    public CashierShiftResponse openShift(String email, OpenCashierShiftRequest request) {
        StaffProfile cashier = findCashier(email);
        if (cashierShiftRepository.findByStaffIdAndStatus(
                cashier.getUserId(), CashierShiftStatus.OPEN
        ).isPresent()) {
            throw new InvalidDataException("Nhan vien da co ca thu ngan OPEN");
        }

        Venue venue = venueRepository.findByIdAndStatus(request.venueId(), VenueStatus.ACTIVE)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay co so dang hoat dong"));
        CashierShift shift = CashierShift.builder()
                .staff(cashier.getUser())
                .venue(venue)
                .openedAt(ZonedDateTime.now())
                .openingCash(requireNonNegativeMoney(request.openingCash(), "Tien dau ca"))
                .status(CashierShiftStatus.OPEN)
                .notes(formatNote("Mo ca", request.notes()))
                .build();
        try {
            return toResponse(cashierShiftRepository.saveAndFlush(shift), cashier);
        } catch (DataIntegrityViolationException exception) {
            throw new InvalidDataException("Nhan vien da co ca thu ngan OPEN");
        }
    }

    @Transactional(readOnly = true)
    public OpenShiftStatusResponse getOpenStatus(String email) {
        StaffProfile cashier = findCashier(email);
        return cashierShiftRepository.findByStaffIdAndStatus(cashier.getUserId(), CashierShiftStatus.OPEN)
                .map(shift -> new OpenShiftStatusResponse(true, shift.getId(), shift.getVenue().getId()))
                .orElseGet(() -> new OpenShiftStatusResponse(false, null, null));
    }

    @Transactional(readOnly = true)
    public CashierShiftResponse getCurrentShift(String email) {
        StaffProfile cashier = findCashier(email);
        return toResponse(findOpenShift(cashier.getUserId()), cashier);
    }

    @Transactional(readOnly = true)
    public List<CashMovementResponse> getCurrentMovements(String email) {
        StaffProfile cashier = findCashier(email);
        CashierShift shift = findOpenShift(cashier.getUserId());
        return cashMovementRepository.findByCashierShiftIdOrderByCreatedAtAsc(shift.getId())
                .stream()
                .map(this::toMovementResponse)
                .toList();
    }

    @Transactional
    public CashMovementResponse recordCashOut(String email, CashOutRequest request) {
        StaffProfile cashier = findCashier(email);
        CashierShift shift = findOpenShiftForUpdate(cashier.getUserId());
        BigDecimal amount = requirePositiveMoney(request.amount(), "So tien chi");
        BigDecimal availableCash = calculateExpectedCash(shift);
        if (amount.compareTo(availableCash) > 0) {
            throw new InvalidDataException("So tien chi vuot qua tien mat du kien trong ca");
        }

        CashMovement movement = CashMovement.builder()
                .cashierShift(shift)
                .movementType(CashMovementType.CASH_OUT)
                .sourceType(CashMovementSourceType.CASH_ADJUSTMENT)
                .amount(amount)
                .description(request.description().trim())
                .createdBy(cashier.getUser())
                .build();
        return toMovementResponse(cashMovementRepository.save(movement));
    }

    @Transactional
    public CashierShiftResponse closeShift(String email, CloseCashierShiftRequest request) {
        StaffProfile cashier = findCashier(email);
        CashierShift shift = findOpenShiftForUpdate(cashier.getUserId());
        BigDecimal expected = calculateExpectedCash(shift);
        BigDecimal actual = requireNonNegativeMoney(request.actualClosingCash(), "Tien cuoi ca thuc te");
        BigDecimal difference = money(actual.subtract(expected));
        String closingNote = trimToNull(request.notes());
        if (difference.compareTo(BigDecimal.ZERO) != 0 && closingNote == null) {
            throw new InvalidDataException("Phai ghi chu nguyen nhan khi tien cuoi ca co chenh lech");
        }

        shift.setExpectedClosingCash(expected);
        shift.setActualClosingCash(actual);
        shift.setCashDifference(difference);
        shift.setClosedAt(ZonedDateTime.now());
        shift.setStatus(CashierShiftStatus.CLOSED);
        shift.setNotes(appendNote(shift.getNotes(), formatNote("Dong ca", closingNote)));
        return toResponse(cashierShiftRepository.save(shift), cashier);
    }

    @Transactional(readOnly = true)
    public List<CashierShiftResponse> getMyHistory(String email, LocalDate from, LocalDate to) {
        StaffProfile cashier = findCashier(email);
        DateRange range = dateRange(from, to);
        return cashierShiftRepository.findHistoryByStaff(
                        cashier.getUserId(), range.fromTime(), range.toTime()
                )
                .stream()
                .map(shift -> toResponse(shift, cashier))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CashierShiftResponse> getAllShifts(
            Long staffId,
            Long venueId,
            CashierShiftStatus status,
            LocalDate from,
            LocalDate to
    ) {
        DateRange range = dateRange(from, to);
        return cashierShiftRepository.findForManagement(
                        staffId, venueId, status, range.fromTime(), range.toTime()
                )
                .stream()
                .map(shift -> toResponse(shift, findStaffProfile(shift.getStaff().getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CashierShiftResponse getShiftDetails(Long id) {
        CashierShift shift = cashierShiftRepository.findDetailedById(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay ca thu ngan"));
        return toResponse(shift, findStaffProfile(shift.getStaff().getId()));
    }

    private StaffProfile findCashier(String email) {
        StaffProfile profile = staffProfileRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay ho so nhan vien"));
        if (!"CASHIER".equalsIgnoreCase(profile.getPosition())) {
            throw new InvalidDataException("Nhan vien khong co vi tri CASHIER");
        }
        return profile;
    }

    private StaffProfile findStaffProfile(Long staffId) {
        return staffProfileRepository.findByUserId(staffId)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay ho so nhan vien"));
    }

    private CashierShift findOpenShift(Long staffId) {
        return cashierShiftRepository.findByStaffIdAndStatus(staffId, CashierShiftStatus.OPEN)
                .orElseThrow(() -> new InvalidDataException("Nhan vien khong co ca thu ngan OPEN"));
    }

    private CashierShift findOpenShiftForUpdate(Long staffId) {
        return cashierShiftRepository.findByStaffIdAndStatusForUpdate(staffId, CashierShiftStatus.OPEN)
                .orElseThrow(() -> new InvalidDataException("Nhan vien khong co ca thu ngan OPEN"));
    }

    private CashierShiftResponse toResponse(CashierShift shift, StaffProfile profile) {
        BigDecimal cashIn = sumMovement(shift.getId(), CashMovementType.CASH_IN);
        BigDecimal cashOut = sumMovement(shift.getId(), CashMovementType.CASH_OUT);
        BigDecimal expected = shift.getExpectedClosingCash() == null
                ? money(shift.getOpeningCash().add(cashIn).subtract(cashOut))
                : money(shift.getExpectedClosingCash());
        return new CashierShiftResponse(
                shift.getId(),
                shift.getStaff().getId(),
                profile.getEmployeeCode(),
                shift.getStaff().getFullName(),
                shift.getVenue().getId(),
                shift.getVenue().getName(),
                shift.getOpenedAt(),
                shift.getClosedAt(),
                money(shift.getOpeningCash()),
                cashIn,
                cashOut,
                expected,
                nullableMoney(shift.getActualClosingCash()),
                nullableMoney(shift.getCashDifference()),
                shift.getStatus(),
                shift.getNotes(),
                shift.getCreatedAt()
        );
    }

    private CashMovementResponse toMovementResponse(CashMovement movement) {
        UserEntity creator = movement.getCreatedBy();
        return new CashMovementResponse(
                movement.getId(),
                movement.getCashierShift().getId(),
                movement.getMovementType(),
                movement.getSourceType(),
                money(movement.getAmount()),
                movement.getReferenceId(),
                movement.getDescription(),
                creator.getId(),
                creator.getFullName(),
                movement.getCreatedAt()
        );
    }

    private BigDecimal calculateExpectedCash(CashierShift shift) {
        return money(shift.getOpeningCash()
                .add(sumMovement(shift.getId(), CashMovementType.CASH_IN))
                .subtract(sumMovement(shift.getId(), CashMovementType.CASH_OUT)));
    }

    private BigDecimal sumMovement(Long shiftId, CashMovementType type) {
        BigDecimal amount = cashMovementRepository.sumAmountByShiftAndType(shiftId, type);
        return money(amount == null ? BigDecimal.ZERO : amount);
    }

    private BigDecimal requirePositiveMoney(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDataException(fieldName + " phai lon hon 0");
        }
        return exactMoney(value, fieldName);
    }

    private BigDecimal requireNonNegativeMoney(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidDataException(fieldName + " khong duoc am");
        }
        return exactMoney(value, fieldName);
    }

    private BigDecimal exactMoney(BigDecimal value, String fieldName) {
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvalidDataException(fieldName + " chi duoc co toi da 2 chu so thap phan");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullableMoney(BigDecimal value) {
        return value == null ? null : money(value);
    }

    private DateRange dateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDataException("Ngay bat dau khong duoc sau ngay ket thuc");
        }
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime fromTime = from == null ? null : from.atStartOfDay(zone);
        ZonedDateTime toTime = to == null ? null : to.plusDays(1).atStartOfDay(zone);
        return new DateRange(fromTime, toTime);
    }

    private String formatNote(String label, String note) {
        String normalized = trimToNull(note);
        return normalized == null ? null : label + ": " + normalized;
    }

    private String appendNote(String current, String additional) {
        if (additional == null) {
            return current;
        }
        return current == null ? additional : current + System.lineSeparator() + additional;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DateRange(ZonedDateTime fromTime, ZonedDateTime toTime) {
    }
}
