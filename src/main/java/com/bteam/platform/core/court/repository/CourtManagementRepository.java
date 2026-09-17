package com.bteam.platform.core.court.repository;

import com.bteam.platform.core.venue.model.Court;
import com.bteam.platform.core.venue.model.CourtStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface CourtManagementRepository extends JpaRepository<Court, Long> {
    List<Court> findAllByOrderByVenueNameAscCourtCodeAsc();

    List<Court> findByVenueIdOrderByCourtCodeAsc(Long venueId);

    Optional<Court> findByVenueIdAndCourtCodeIgnoreCase(Long venueId, String courtCode);

    List<Court> findByPricePerHourBetweenOrderByPricePerHourAsc(BigDecimal minPrice, BigDecimal maxPrice);

    @Query("""
            select c
            from Court c
            where lower(c.name) like lower(concat('%', :keyword, '%'))
               or lower(c.courtCode) like lower(concat('%', :keyword, '%'))
               or lower(c.venue.name) like lower(concat('%', :keyword, '%'))
            order by c.venue.name asc, c.courtCode asc
            """)
    List<Court> searchCourts(@Param("keyword") String keyword);

    @Query(value = """
            select count(*)
            from booking_details bd
            join bookings b on b.id = bd.booking_id
            where bd.court_id = :courtId
              and b.booking_date = :bookingDate
              and b.status not in ('CANCELLED')
              and bd.status not in ('CANCELLED')
              and bd.start_time < :endTime
              and bd.end_time > :startTime
            """, nativeQuery = true)
    long countOverlappingBookings(
            @Param("courtId") Long courtId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    long countByStatus(CourtStatus status);
}
