package com.bteam.platform.core.payment.repository;

import com.bteam.platform.core.payment.model.CashierShift;
import com.bteam.platform.core.payment.model.CashierShiftStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface CashierShiftRepository extends JpaRepository<CashierShift, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"staff", "venue"})
    Optional<CashierShift> findByStaffIdAndVenueIdAndStatus(
            Long staffId,
            Long venueId,
            CashierShiftStatus status
    );

    @EntityGraph(attributePaths = {"staff", "venue"})
    Optional<CashierShift> findByStaffIdAndStatus(Long staffId, CashierShiftStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select cs
            from CashierShift cs
            join fetch cs.staff
            join fetch cs.venue
            where cs.staff.id = :staffId and cs.status = :status
            """)
    Optional<CashierShift> findByStaffIdAndStatusForUpdate(
            @Param("staffId") Long staffId,
            @Param("status") CashierShiftStatus status
    );

    @EntityGraph(attributePaths = {"staff", "venue"})
    @Query("select cs from CashierShift cs where cs.id = :id")
    Optional<CashierShift> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"staff", "venue"})
    @Query("""
            select cs
            from CashierShift cs
            where cs.staff.id = :staffId
              and (:fromTime is null or cs.openedAt >= :fromTime)
              and (:toTime is null or cs.openedAt < :toTime)
            order by cs.openedAt desc
            """)
    List<CashierShift> findHistoryByStaff(
            @Param("staffId") Long staffId,
            @Param("fromTime") ZonedDateTime fromTime,
            @Param("toTime") ZonedDateTime toTime
    );

    @EntityGraph(attributePaths = {"staff", "venue"})
    @Query("""
            select cs
            from CashierShift cs
            where (:staffId is null or cs.staff.id = :staffId)
              and (:venueId is null or cs.venue.id = :venueId)
              and (:status is null or cs.status = :status)
              and (:fromTime is null or cs.openedAt >= :fromTime)
              and (:toTime is null or cs.openedAt < :toTime)
            order by cs.openedAt desc
            """)
    List<CashierShift> findForManagement(
            @Param("staffId") Long staffId,
            @Param("venueId") Long venueId,
            @Param("status") CashierShiftStatus status,
            @Param("fromTime") ZonedDateTime fromTime,
            @Param("toTime") ZonedDateTime toTime
    );
}
