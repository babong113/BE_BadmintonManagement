package com.bteam.platform.core.booking.repository;

import com.bteam.platform.core.booking.model.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByBookingCode(String bookingCode);

    @EntityGraph(attributePaths = {"venue", "customer", "createdBy", "details", "details.court", "details.court.venue"})
    Optional<Booking> findWithDetailsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findWithDetailsByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Booking b
            set b.status = com.bteam.platform.core.booking.model.BookingStatus.EXPIRED,
                b.updatedAt = CURRENT_TIMESTAMP
            where b.status = com.bteam.platform.core.booking.model.BookingStatus.PENDING
              and b.createdAt <= :cutoff
            """)
    int expireOverduePendingBookings(@Param("cutoff") ZonedDateTime cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Booking b
            set b.status = com.bteam.platform.core.booking.model.BookingStatus.EXPIRED,
                b.updatedAt = CURRENT_TIMESTAMP
            where b.id = :id
              and b.status = com.bteam.platform.core.booking.model.BookingStatus.PENDING
              and b.createdAt <= :cutoff
            """)
    int expireOverduePendingBooking(
            @Param("id") Long id,
            @Param("cutoff") ZonedDateTime cutoff
    );

    @EntityGraph(attributePaths = {"venue", "customer", "createdBy", "details", "details.court", "details.court.venue"})
    List<Booking> findByCustomerIdOrderByBookingDateDescCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"venue", "customer", "createdBy", "details", "details.court", "details.court.venue"})
    @Query("""
            select distinct b
            from Booking b
            join b.details d
            join d.court c
            where b.bookingDate = :bookingDate
              and (:courtId is null or c.id = :courtId)
              and (:venueId is null or c.venue.id = :venueId)
            order by b.createdAt desc
            """)
    List<Booking> findSchedule(
            @Param("bookingDate") LocalDate bookingDate,
            @Param("courtId") Long courtId,
            @Param("venueId") Long venueId
    );
}
