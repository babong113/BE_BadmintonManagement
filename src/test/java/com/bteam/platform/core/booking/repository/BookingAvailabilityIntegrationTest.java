package com.bteam.platform.core.booking.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BookingAvailabilityIntegrationTest {
    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void overlapOnlyCountsConfirmedAndPendingWithinFifteenMinutes() {
        Long venueId = jdbcTemplate.queryForObject("""
                insert into venues (name, address, status)
                values ('Venue BR26', 'Test address', 'ACTIVE')
                returning id
                """, Long.class);
        Long courtId = jdbcTemplate.queryForObject("""
                insert into courts (venue_id, court_code, name, price_per_hour, status)
                values (?, 'BR26-C01', 'Court BR26', 100000.00, 'AVAILABLE')
                returning id
                """, Long.class, venueId);
        Long customerId = jdbcTemplate.queryForObject("""
                insert into users (email, password_hash, full_name, status)
                values ('br26@example.com', 'hash', 'BR26 Customer', 'ACTIVE')
                returning id
                """, Long.class);

        insertBooking(courtId, venueId, customerId, "BKG-BR26-CONFIRMED", "CONFIRMED", "CURRENT_TIMESTAMP");
        insertBooking(courtId, venueId, customerId, "BKG-BR26-PENDING-NEW", "PENDING", "CURRENT_TIMESTAMP - INTERVAL '5 minutes'");
        insertBooking(courtId, venueId, customerId, "BKG-BR26-PENDING-OLD", "PENDING", "CURRENT_TIMESTAMP - INTERVAL '16 minutes'");
        insertBooking(courtId, venueId, customerId, "BKG-BR26-CANCELLED", "CANCELLED", "CURRENT_TIMESTAMP");
        insertBooking(courtId, venueId, customerId, "BKG-BR26-EXPIRED", "EXPIRED", "CURRENT_TIMESTAMP");

        long overlaps = bookingDetailRepository.countOverlappingBookings(
                courtId,
                LocalDate.of(2026, 10, 1),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                null
        );

        assertThat(overlaps).isEqualTo(2L);

        int expiredCount = bookingRepository.expireOverduePendingBookings(
                ZonedDateTime.now().minusMinutes(15)
        );
        String expiredStatus = jdbcTemplate.queryForObject(
                "select status from bookings where booking_code = 'BKG-BR26-PENDING-OLD'",
                String.class
        );

        assertThat(expiredCount).isGreaterThanOrEqualTo(1);
        assertThat(expiredStatus).isEqualTo("EXPIRED");
    }

    private void insertBooking(
            Long courtId,
            Long venueId,
            Long customerId,
            String bookingCode,
            String status,
            String createdAtExpression
    ) {
        Long bookingId = jdbcTemplate.queryForObject("""
                insert into bookings (
                    booking_code, venue_id, customer_id, booking_date,
                    total_amount, status, created_at, updated_at
                )
                values (?, ?, ?, '2026-10-01', 100000.00, ?, %s, %s)
                returning id
                """.formatted(createdAtExpression, createdAtExpression),
                Long.class,
                bookingCode,
                venueId,
                customerId,
                status
        );
        jdbcTemplate.update("""
                insert into booking_details (
                    booking_id, court_id, start_time, end_time,
                    unit_price, subtotal, status
                )
                values (?, ?, '18:00:00', '19:00:00', ?, ?, 'ACTIVE')
                """, bookingId, courtId, new BigDecimal("100000.00"), new BigDecimal("100000.00"));
    }
}
