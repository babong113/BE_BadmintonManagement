package com.bteam.platform.core.booking.service;

import com.bteam.platform.core.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class BookingExpirationService {
    public static final Duration HOLD_DURATION = Duration.ofMinutes(15);

    private final BookingRepository bookingRepository;

    @Scheduled(fixedDelayString = "${booking.expiration-check-ms:60000}")
    @Transactional
    public void expireOverdueBookingsOnSchedule() {
        bookingRepository.expireOverduePendingBookings(expirationCutoff());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int expireOverdueBookings() {
        return bookingRepository.expireOverduePendingBookings(expirationCutoff());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expireBookingIfOverdue(Long bookingId) {
        return bookingRepository.expireOverduePendingBooking(bookingId, expirationCutoff()) > 0;
    }

    private ZonedDateTime expirationCutoff() {
        return ZonedDateTime.now().minus(HOLD_DURATION);
    }
}
