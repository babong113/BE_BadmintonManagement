package com.bteam.platform.core.booking.service;

import com.bteam.platform.adapter.persistence.jpa.entity.UserEntity;
import com.bteam.platform.core.booking.dto.BookingDetailRequest;
import com.bteam.platform.core.booking.dto.ChangeBookingVenueRequest;
import com.bteam.platform.core.booking.dto.CreateBookingRequest;
import com.bteam.platform.core.booking.model.Booking;
import com.bteam.platform.core.booking.model.BookingDetail;
import com.bteam.platform.core.booking.model.BookingDetailStatus;
import com.bteam.platform.core.booking.model.BookingStatus;
import com.bteam.platform.core.booking.repository.BookingCourtRepository;
import com.bteam.platform.core.booking.repository.BookingDetailRepository;
import com.bteam.platform.core.booking.repository.BookingRepository;
import com.bteam.platform.core.booking.repository.BookingUserRepository;
import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.payment.model.CashierShift;
import com.bteam.platform.core.payment.model.CashierShiftStatus;
import com.bteam.platform.core.payment.repository.CashierShiftRepository;
import com.bteam.platform.core.venue.model.Court;
import com.bteam.platform.core.venue.model.CourtStatus;
import com.bteam.platform.core.venue.model.Venue;
import com.bteam.platform.core.venue.model.VenueStatus;
import com.bteam.platform.core.venue.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingDetailRepository bookingDetailRepository;
    @Mock
    private BookingCourtRepository courtRepository;
    @Mock
    private BookingUserRepository userRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private BookingExpirationService bookingExpirationService;
    @Mock
    private CashierShiftRepository cashierShiftRepository;

    private BookingService bookingService;
    private UserEntity customer;
    private Venue venueOne;
    private Venue venueTwo;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository,
                bookingDetailRepository,
                courtRepository,
                userRepository,
                venueRepository,
                bookingExpirationService,
                cashierShiftRepository
        );

        customer = new UserEntity();
        customer.setId(10L);
        customer.setEmail("customer@example.com");
        customer.setFullName("Customer");

        venueOne = venue(1L, "Co so 1");
        venueTwo = venue(2L, "Co so 2");
    }

    @Test
    void createBookingStoresSelectedVenue() {
        Court court = court(11L, venueOne);
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(venueRepository.findByIdAndStatus(1L, VenueStatus.ACTIVE)).thenReturn(Optional.of(venueOne));
        when(courtRepository.findById(11L)).thenReturn(Optional.of(court));
        when(bookingDetailRepository.countOverlappingBookings(any(), any(), any(), any(), any())).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(100L);
            return booking;
        });

        var response = bookingService.createBooking(createRequest(1L, 11L), customerAuthentication());

        assertThat(response.venueId()).isEqualTo(1L);
        assertThat(response.venueName()).isEqualTo("Co so 1");
        assertThat(response.details()).hasSize(1);
    }

    @Test
    void createBookingRejectsCourtFromAnotherVenue() {
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(venueRepository.findByIdAndStatus(1L, VenueStatus.ACTIVE)).thenReturn(Optional.of(venueOne));
        when(courtRepository.findById(22L)).thenReturn(Optional.of(court(22L, venueTwo)));

        assertThatThrownBy(() -> bookingService.createBooking(
                createRequest(1L, 22L), customerAuthentication()
        ))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Tat ca san trong booking phai thuoc co so da chon");
    }

    @Test
    void changeBookingVenueRejectsExistingCourtFromAnotherVenue() {
        Booking booking = bookingWithCourt(venueOne, court(11L, venueOne));
        when(bookingRepository.findWithDetailsById(100L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(venueRepository.findByIdAndStatus(2L, VenueStatus.ACTIVE)).thenReturn(Optional.of(venueTwo));

        assertThatThrownBy(() -> bookingService.changeBookingVenue(
                100L,
                new ChangeBookingVenueRequest(2L),
                customerAuthentication()
        ))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Tat ca san trong booking phai thuoc co so da chon");
    }

    @Test
    void expiredPendingBookingCannotBeConfirmed() {
        Booking booking = bookingWithCourt(venueOne, court(11L, venueOne));
        booking.setStatus(BookingStatus.EXPIRED);
        when(bookingRepository.findWithDetailsByIdForUpdate(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmBooking(100L, staffAuthentication()))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Booking da het thoi gian giu san 15 phut");
    }

    @Test
    void staffWithOpenShiftAtBookingVenueCanConfirmWithinHoldTime() {
        Booking booking = bookingWithCourt(venueOne, court(11L, venueOne));
        UserEntity staff = staffUser();
        when(bookingRepository.findWithDetailsByIdForUpdate(100L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(cashierShiftRepository.findByStaffIdAndVenueIdAndStatus(
                20L, 1L, CashierShiftStatus.OPEN
        )).thenReturn(Optional.of(CashierShift.builder().id(50L).staff(staff).venue(venueOne).build()));
        when(bookingRepository.save(booking)).thenReturn(booking);

        var response = bookingService.confirmBooking(100L, staffAuthentication());

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void staffCannotConfirmBookingWithoutOpenShiftAtBookingVenue() {
        Booking booking = bookingWithCourt(venueOne, court(11L, venueOne));
        UserEntity staff = staffUser();
        when(bookingRepository.findWithDetailsByIdForUpdate(100L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(cashierShiftRepository.findByStaffIdAndVenueIdAndStatus(
                20L, 1L, CashierShiftStatus.OPEN
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirmBooking(100L, staffAuthentication()))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Nhan vien phai co ca OPEN tai dung co so cua booking");
    }

    private CreateBookingRequest createRequest(Long venueId, Long courtId) {
        return new CreateBookingRequest(
                venueId,
                null,
                null,
                null,
                LocalDate.now().plusDays(1),
                "Test booking venue",
                List.of(new BookingDetailRequest(courtId, LocalTime.of(18, 0), LocalTime.of(19, 0)))
        );
    }

    private Booking bookingWithCourt(Venue venue, Court court) {
        Booking booking = Booking.builder()
                .id(100L)
                .bookingCode("BKG-100")
                .venue(venue)
                .customer(customer)
                .bookingDate(LocalDate.now().plusDays(1))
                .status(BookingStatus.PENDING)
                .build();
        BookingDetail detail = BookingDetail.builder()
                .id(200L)
                .booking(booking)
                .court(court)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .unitPrice(court.getPricePerHour())
                .subtotal(court.getPricePerHour())
                .status(BookingDetailStatus.ACTIVE)
                .build();
        booking.setDetails(List.of(detail));
        return booking;
    }

    private Venue venue(Long id, String name) {
        return Venue.builder()
                .id(id)
                .name(name)
                .address("Dia chi")
                .status(VenueStatus.ACTIVE)
                .build();
    }

    private Court court(Long id, Venue venue) {
        return Court.builder()
                .id(id)
                .venue(venue)
                .courtCode("C" + id)
                .name("San " + id)
                .pricePerHour(new BigDecimal("120000.00"))
                .status(CourtStatus.AVAILABLE)
                .build();
    }

    private Authentication customerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                customer.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("booking:create"))
        );
    }

    private UserEntity staffUser() {
        UserEntity staff = new UserEntity();
        staff.setId(20L);
        staff.setEmail("staff@example.com");
        staff.setFullName("Staff");
        return staff;
    }

    private Authentication staffAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "staff@example.com",
                null,
                List.of(new SimpleGrantedAuthority("booking:update"))
        );
    }
}
