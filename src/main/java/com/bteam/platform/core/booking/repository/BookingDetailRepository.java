package com.bteam.platform.core.booking.repository;

import com.bteam.platform.core.booking.model.BookingDetail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from BookingDetail d where d.id = :id")
    Optional<BookingDetail> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select d
            from BookingDetail d
            join fetch d.booking b
            join fetch b.venue
            join fetch d.court c
            join fetch c.venue
            left join fetch b.customer
            left join fetch b.createdBy
            where d.id = :id
            """)
    Optional<BookingDetail> findWithBookingById(@Param("id") Long id);

    @Query(value = """
            select count(*)
            from booking_details bd
            join bookings b on b.id = bd.booking_id
            where bd.court_id = :courtId
              and b.booking_date = :bookingDate
              and (
                  b.status = 'CONFIRMED'
                  or (
                      b.status = 'PENDING'
                      and b.created_at > CURRENT_TIMESTAMP - INTERVAL '15 minutes'
                  )
              )
              and bd.status not in ('CANCELLED')
              and (:ignoredDetailId is null or bd.id <> :ignoredDetailId)
              and bd.start_time < :endTime
              and bd.end_time > :startTime
            """, nativeQuery = true)
    long countOverlappingBookings(
            @Param("courtId") Long courtId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("ignoredDetailId") Long ignoredDetailId
    );
}
