package com.bteam.platform.core.booking.service;

import com.bteam.platform.adapter.persistence.jpa.entity.UserEntity;
import com.bteam.platform.core.booking.dto.AvailableCourtResponse;
import com.bteam.platform.core.booking.dto.BookingDetailRequest;
import com.bteam.platform.core.booking.dto.BookingDetailResponse;
import com.bteam.platform.core.booking.dto.BookingResponse;
import com.bteam.platform.core.booking.dto.BookingVenueResponse;
import com.bteam.platform.core.booking.dto.ChangeBookingVenueRequest;
import com.bteam.platform.core.booking.dto.ChangeCourtRequest;
import com.bteam.platform.core.booking.dto.ChangeTimeRequest;
import com.bteam.platform.core.booking.dto.CreateBookingRequest;
import com.bteam.platform.core.booking.dto.UpdateBookingRequest;
import com.bteam.platform.core.booking.model.Booking;
import com.bteam.platform.core.booking.model.BookingDetail;
import com.bteam.platform.core.booking.model.BookingDetailStatus;
import com.bteam.platform.core.booking.model.BookingStatus;
import com.bteam.platform.core.booking.repository.BookingCourtRepository;
import com.bteam.platform.core.booking.repository.BookingDetailRepository;
import com.bteam.platform.core.booking.repository.BookingRepository;
import com.bteam.platform.core.booking.repository.BookingUserRepository;
import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.payment.model.CashierShiftStatus;
import com.bteam.platform.core.payment.repository.CashierShiftRepository;
import com.bteam.platform.core.product.model.BookingEquipment;
import com.bteam.platform.core.venue.model.Court;
import com.bteam.platform.core.venue.model.CourtStatus;
import com.bteam.platform.core.venue.model.Venue;
import com.bteam.platform.core.venue.model.VenueStatus;
import com.bteam.platform.core.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter BOOKING_CODE_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final BookingCourtRepository courtRepository;
    private final BookingUserRepository userRepository;
    private final VenueRepository venueRepository;
    private final BookingExpirationService bookingExpirationService;
    private final CashierShiftRepository cashierShiftRepository;

    @Transactional
    public List<AvailableCourtResponse> findAvailableCourts(
            Long venueId,
            LocalDate bookingDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        bookingExpirationService.expireOverdueBookings();
        validateDateAndTime(bookingDate, startTime, endTime);

        return courtRepository.findByStatusOrderByVenueNameAscCourtCodeAsc(CourtStatus.AVAILABLE)
                .stream()
                .filter(court -> venueId == null || court.getVenue().getId().equals(venueId))
                .filter(court -> court.getVenue().getStatus() == VenueStatus.ACTIVE)
                .filter(court -> isWithinOperatingHours(court.getVenue(), startTime, endTime))
                .filter(court -> isCourtAvailable(court.getId(), bookingDate, startTime, endTime, null))
                .map(court -> new AvailableCourtResponse(
                        court.getId(),
                        court.getVenue().getId(),
                        court.getVenue().getName(),
                        court.getCourtCode(),
                        court.getName(),
                        court.getPricePerHour()
                ))
                .toList();
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, Authentication authentication) {
        UserEntity actor = findUserByEmail(authentication.getName());
        Set<String> authorities = authorities(authentication);
        Venue venue = findActiveVenue(request.venueId());

        Booking booking = Booking.builder()
                .bookingCode(generateBookingCode())
                .venue(venue)
                .bookingDate(request.bookingDate())
                .note(trimToNull(request.note()))
                .status(BookingStatus.PENDING)
                .createdBy(actor)
                .build();

        applyCustomerIdentity(booking, request, actor, authorities);

        for (BookingDetailRequest detailRequest : request.details()) {
            booking.addDetail(createDetail(booking.getBookingDate(), venue.getId(), detailRequest, null));
        }

        recalculateTotal(booking);
        return toBookingResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse confirmBooking(Long id, Authentication authentication) {
        bookingExpirationService.expireBookingIfOverdue(id);
        Booking booking = bookingRepository.findWithDetailsByIdForUpdate(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay booking"));
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new InvalidDataException("Booking da het thoi gian giu san 15 phut");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidDataException("Chi booking PENDING moi duoc xac nhan");
        }
        ensureStaffCanConfirmAtVenue(booking, authentication);

        booking.setStatus(BookingStatus.CONFIRMED);
        return toBookingResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(Authentication authentication) {
        bookingExpirationService.expireOverdueBookings();
        UserEntity actor = findUserByEmail(authentication.getName());
        return bookingRepository.findByCustomerIdOrderByBookingDateDescCreatedAtDesc(actor.getId())
                .stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getSchedule(LocalDate bookingDate, Long courtId, Long venueId) {
        bookingExpirationService.expireOverdueBookings();
        if (bookingDate == null) {
            throw new InvalidDataException("Ngay dat san khong duoc de trong");
        }

        return bookingRepository.findSchedule(bookingDate, courtId, venueId)
                .stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingDetail(Long id, Authentication authentication) {
        bookingExpirationService.expireBookingIfOverdue(id);
        Booking booking = findBooking(id);
        ensureCanReadBooking(booking, authentication);
        return toBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingVenueResponse getBookingVenue(Long id, Authentication authentication) {
        bookingExpirationService.expireBookingIfOverdue(id);
        Booking booking = findBooking(id);
        ensureCanReadBooking(booking, authentication);
        return toBookingVenueResponse(booking);
    }

    @Transactional
    public BookingVenueResponse changeBookingVenue(
            Long id,
            ChangeBookingVenueRequest request,
            Authentication authentication
    ) {
        bookingExpirationService.expireBookingIfOverdue(id);
        Booking booking = findBooking(id);
        ensureBookingCanChange(booking);
        ensureCanChangeBookingVenue(booking, authentication);

        Venue venue = findActiveVenue(request.venueId());
        boolean containsCourtFromAnotherVenue = booking.getDetails().stream()
                .anyMatch(detail -> !detail.getCourt().getVenue().getId().equals(venue.getId()));
        if (containsCourtFromAnotherVenue) {
            throw new InvalidDataException("Tat ca san trong booking phai thuoc co so da chon");
        }

        booking.setVenue(venue);
        return toBookingVenueResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse updateBooking(Long id, UpdateBookingRequest request) {
        bookingExpirationService.expireBookingIfOverdue(id);
        Booking booking = findBooking(id);
        ensureBookingCanChange(booking);

        booking.setGuestName(trimToNull(request.guestName()));
        booking.setGuestPhone(trimToNull(request.guestPhone()));
        booking.setNote(trimToNull(request.note()));

        if (booking.getCustomer() == null && (booking.getGuestName() == null || booking.getGuestPhone() == null)) {
            throw new InvalidDataException("Booking khach vang lai can ten va so dien thoai");
        }

        return toBookingResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse changeCourt(Long detailId, ChangeCourtRequest request) {
        bookingExpirationService.expireOverdueBookings();
        BookingDetail detail = findDetail(detailId);
        ensureBookingCanChange(detail.getBooking());

        Court newCourt = findCourt(request.courtId());
        validateCourtBelongsToVenue(newCourt, detail.getBooking().getVenue().getId());
        validateCourtBookable(newCourt, detail.getBooking().getBookingDate(), detail.getStartTime(), detail.getEndTime(), detail.getId());

        detail.setCourt(newCourt);
        detail.setUnitPrice(newCourt.getPricePerHour());
        detail.setSubtotal(calculateSubtotal(newCourt.getPricePerHour(), detail.getStartTime(), detail.getEndTime()));
        recalculateTotal(detail.getBooking());

        bookingDetailRepository.save(detail);
        return toBookingResponse(findBooking(detail.getBooking().getId()));
    }

    @Transactional
    public BookingResponse changeTime(Long detailId, ChangeTimeRequest request) {
        bookingExpirationService.expireOverdueBookings();
        BookingDetail detail = findDetail(detailId);
        ensureBookingCanChange(detail.getBooking());
        validateDateAndTime(detail.getBooking().getBookingDate(), request.startTime(), request.endTime());
        validateCourtBookable(detail.getCourt(), detail.getBooking().getBookingDate(), request.startTime(), request.endTime(), detail.getId());

        detail.setStartTime(request.startTime());
        detail.setEndTime(request.endTime());
        detail.setSubtotal(calculateSubtotal(detail.getUnitPrice(), request.startTime(), request.endTime()));
        recalculateTotal(detail.getBooking());

        bookingDetailRepository.save(detail);
        return toBookingResponse(findBooking(detail.getBooking().getId()));
    }

    @Transactional
    public BookingResponse cancelBooking(Long id, Authentication authentication) {
        bookingExpirationService.expireBookingIfOverdue(id);
        Booking booking = findBooking(id);
        ensureCanCancelBooking(booking, authentication);
        ensureBookingCanChange(booking);

        booking.setStatus(BookingStatus.CANCELLED);
        booking.getDetails().forEach(detail -> detail.setStatus(BookingDetailStatus.CANCELLED));
        recalculateTotal(booking);

        return toBookingResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse cancelBookingDetail(Long detailId) {
        bookingExpirationService.expireOverdueBookings();
        BookingDetail detail = findDetail(detailId);
        ensureBookingCanChange(detail.getBooking());

        detail.setStatus(BookingDetailStatus.CANCELLED);
        recalculateTotal(detail.getBooking());

        if (detail.getBooking().getDetails()
                .stream()
                .allMatch(item -> item.getStatus() == BookingDetailStatus.CANCELLED)) {
            detail.getBooking().setStatus(BookingStatus.CANCELLED);
        }

        bookingDetailRepository.save(detail);
        return toBookingResponse(findBooking(detail.getBooking().getId()));
    }

    @Transactional
    public BookingResponse completeBooking(Long id) {
        bookingExpirationService.expireBookingIfOverdue(id);
        Booking booking = findBooking(id);
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new InvalidDataException("Khong the hoan tat booking da huy hoac het han");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.getDetails()
                .stream()
                .filter(detail -> detail.getStatus() == BookingDetailStatus.ACTIVE)
                .forEach(detail -> detail.setStatus(BookingDetailStatus.COMPLETED));

        return toBookingResponse(bookingRepository.save(booking));
    }

    private BookingDetail createDetail(
            LocalDate bookingDate,
            Long venueId,
            BookingDetailRequest request,
            Long ignoredDetailId
    ) {
        validateDateAndTime(bookingDate, request.startTime(), request.endTime());
        Court court = findCourt(request.courtId());
        validateCourtBelongsToVenue(court, venueId);
        validateCourtBookable(court, bookingDate, request.startTime(), request.endTime(), ignoredDetailId);

        BigDecimal unitPrice = court.getPricePerHour();
        return BookingDetail.builder()
                .court(court)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .unitPrice(unitPrice)
                .subtotal(calculateSubtotal(unitPrice, request.startTime(), request.endTime()))
                .status(BookingDetailStatus.ACTIVE)
                .build();
    }

    private void applyCustomerIdentity(
            Booking booking,
            CreateBookingRequest request,
            UserEntity actor,
            Set<String> authorities
    ) {
        boolean staffFlow = hasAny(authorities, "booking:read", "booking:update", "ROLE_STAFF", "ROLE_OWNER");

        if (!staffFlow) {
            if (request.customerId() != null || request.guestName() != null || request.guestPhone() != null) {
                throw new InvalidDataException("Khach hang chi duoc tao booking cho chinh minh");
            }
            booking.setCustomer(actor);
            return;
        }

        if (request.customerId() != null) {
            booking.setCustomer(userRepository.findById(request.customerId())
                    .orElseThrow(() -> new InvalidDataException("Khong tim thay khach hang")));
            return;
        }

        String guestName = trimToNull(request.guestName());
        String guestPhone = trimToNull(request.guestPhone());
        if (guestName == null || guestPhone == null) {
            throw new InvalidDataException("Can customerId hoac thong tin khach vang lai");
        }

        booking.setGuestName(guestName);
        booking.setGuestPhone(guestPhone);
    }

    private void validateCourtBookable(
            Court court,
            LocalDate bookingDate,
            LocalTime startTime,
            LocalTime endTime,
            Long ignoredDetailId
    ) {
        if (court.getStatus() != CourtStatus.AVAILABLE) {
            throw new InvalidDataException("San khong o trang thai AVAILABLE");
        }
        if (court.getVenue().getStatus() != VenueStatus.ACTIVE) {
            throw new InvalidDataException("Co so cua san khong hoat dong");
        }
        if (!isWithinOperatingHours(court.getVenue(), startTime, endTime)) {
            throw new InvalidDataException("Thoi gian dat nam ngoai gio hoat dong cua co so");
        }
        if (!isCourtAvailable(court.getId(), bookingDate, startTime, endTime, ignoredDetailId)) {
            throw new InvalidDataException("San da co booking trung thoi gian");
        }
    }

    private boolean isCourtAvailable(
            Long courtId,
            LocalDate bookingDate,
            LocalTime startTime,
            LocalTime endTime,
            Long ignoredDetailId
    ) {
        return bookingDetailRepository.countOverlappingBookings(
                courtId,
                bookingDate,
                startTime,
                endTime,
                ignoredDetailId
        ) == 0;
    }

    private boolean isWithinOperatingHours(Venue venue, LocalTime startTime, LocalTime endTime) {
        if (venue.getOpeningTime() != null && startTime.isBefore(venue.getOpeningTime())) {
            return false;
        }
        return venue.getClosingTime() == null || !endTime.isAfter(venue.getClosingTime());
    }

    private void validateDateAndTime(LocalDate bookingDate, LocalTime startTime, LocalTime endTime) {
        if (bookingDate == null || startTime == null || endTime == null) {
            throw new InvalidDataException("Ngay dat san, gio bat dau va gio ket thuc khong duoc de trong");
        }
        if (!endTime.isAfter(startTime)) {
            throw new InvalidDataException("Gio ket thuc phai sau gio bat dau");
        }
    }

    private BigDecimal calculateSubtotal(BigDecimal unitPrice, LocalTime startTime, LocalTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return unitPrice.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private void recalculateTotal(Booking booking) {
        BigDecimal total = booking.getDetails()
                .stream()
                .filter(detail -> detail.getStatus() != BookingDetailStatus.CANCELLED)
                .map(detail -> detail.getSubtotal().add(
                        detail.getEquipmentRentals()
                                .stream()
                                .map(BookingEquipment::getSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        booking.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
    }

    private String generateBookingCode() {
        String prefix = "BKG" + LocalDate.now().format(BOOKING_CODE_DATE_FORMAT);
        String code;
        do {
            code = prefix + "-" + randomDigits(6);
        } while (bookingRepository.existsByBookingCode(code));
        return code;
    }

    private String randomDigits(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findWithDetailsById(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay booking"));
    }

    private BookingDetail findDetail(Long id) {
        return bookingDetailRepository.findWithBookingById(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay booking detail"));
    }

    private Court findCourt(Long id) {
        return courtRepository.findById(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay san"));
    }

    private Venue findActiveVenue(Long id) {
        return venueRepository.findByIdAndStatus(id, VenueStatus.ACTIVE)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay co so dang hoat dong"));
    }

    private void validateCourtBelongsToVenue(Court court, Long venueId) {
        if (!court.getVenue().getId().equals(venueId)) {
            throw new InvalidDataException("Tat ca san trong booking phai thuoc co so da chon");
        }
    }

    private UserEntity findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidDataException("Nguoi dung khong ton tai"));
    }

    private void ensureBookingCanChange(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.COMPLETED
                || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new InvalidDataException("Khong the cap nhat booking da huy, da hoan tat hoac het han");
        }
    }

    private void ensureStaffCanConfirmAtVenue(Booking booking, Authentication authentication) {
        UserEntity staff = findUserByEmail(authentication.getName());
        boolean hasOpenShiftAtVenue = cashierShiftRepository.findByStaffIdAndVenueIdAndStatus(
                staff.getId(), booking.getVenue().getId(), CashierShiftStatus.OPEN
        ).isPresent();
        if (!hasOpenShiftAtVenue) {
            throw new InvalidDataException("Nhan vien phai co ca OPEN tai dung co so cua booking");
        }
    }

    private void ensureCanReadBooking(Booking booking, Authentication authentication) {
        if (hasAuthority(authentication, "booking:read")) {
            return;
        }
        UserEntity actor = findUserByEmail(authentication.getName());
        if (booking.getCustomer() != null && booking.getCustomer().getId().equals(actor.getId())) {
            return;
        }
        throw new InvalidDataException("Ban khong co quyen xem booking nay");
    }

    private void ensureCanChangeBookingVenue(Booking booking, Authentication authentication) {
        if (hasAuthority(authentication, "booking:update")) {
            return;
        }
        UserEntity actor = findUserByEmail(authentication.getName());
        if (hasAuthority(authentication, "booking:create")
                && booking.getCustomer() != null
                && booking.getCustomer().getId().equals(actor.getId())) {
            return;
        }
        throw new InvalidDataException("Ban khong co quyen thay doi co so cua booking nay");
    }

    private void ensureCanCancelBooking(Booking booking, Authentication authentication) {
        if (hasAuthority(authentication, "booking:cancel")) {
            UserEntity actor = findUserByEmail(authentication.getName());
            if (hasAny(authorities(authentication), "booking:read", "booking:update", "ROLE_STAFF", "ROLE_OWNER")
                    || (booking.getCustomer() != null && booking.getCustomer().getId().equals(actor.getId()))) {
                return;
            }
        }
        throw new InvalidDataException("Ban khong co quyen huy booking nay");
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authorities(authentication).contains(authority);
    }

    private boolean hasAny(Set<String> authorities, String... expectedAuthorities) {
        for (String authority : expectedAuthorities) {
            if (authorities.contains(authority)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> authorities(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getVenue().getId(),
                booking.getVenue().getName(),
                booking.getCustomer() == null ? null : booking.getCustomer().getId(),
                booking.getCustomer() == null ? null : booking.getCustomer().getFullName(),
                booking.getCustomer() == null ? null : booking.getCustomer().getPhoneNumber(),
                booking.getGuestName(),
                booking.getGuestPhone(),
                booking.getBookingDate(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getNote(),
                booking.getCreatedBy() == null ? null : booking.getCreatedBy().getId(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                booking.getDetails()
                        .stream()
                        .map(this::toBookingDetailResponse)
                        .toList()
        );
    }

    private BookingVenueResponse toBookingVenueResponse(Booking booking) {
        return new BookingVenueResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getVenue().getId(),
                booking.getVenue().getName()
        );
    }

    private BookingDetailResponse toBookingDetailResponse(BookingDetail detail) {
        Court court = detail.getCourt();
        return new BookingDetailResponse(
                detail.getId(),
                court.getId(),
                court.getCourtCode(),
                court.getName(),
                court.getVenue().getId(),
                court.getVenue().getName(),
                detail.getStartTime(),
                detail.getEndTime(),
                detail.getUnitPrice(),
                detail.getSubtotal(),
                detail.getStatus()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
