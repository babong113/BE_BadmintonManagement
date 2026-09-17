package com.bteam.platform.core.staff.repository;

import com.bteam.platform.core.auth.model.AccountStatus;
import com.bteam.platform.core.staff.model.StaffProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
    @EntityGraph(attributePaths = "user")
    @Query("""
            select sp
            from StaffProfile sp
            where (:status is null or sp.user.status = :status)
            order by sp.user.fullName asc
            """)
    List<StaffProfile> findStaff(@Param("status") AccountStatus status);

    @EntityGraph(attributePaths = "user")
    Optional<StaffProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<StaffProfile> findByUserEmailIgnoreCase(String email);

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);

    boolean existsByEmployeeCodeIgnoreCaseAndUserIdNot(String employeeCode, Long userId);

    boolean existsByIdentityCard(String identityCard);

    boolean existsByIdentityCardAndUserIdNot(String identityCard, Long userId);

    @Query(value = """
            select
                u.id as staffId,
                sp.employee_code as employeeCode,
                u.full_name as fullName,
                u.email as email,
                u.phone_number as phoneNumber,
                sp.position as position,
                cs.id as shiftId,
                cs.opened_at as openedAt,
                cs.status as shiftStatus,
                v.id as venueId,
                v.name as venueName
            from cashier_shifts cs
            join users u on u.id = cs.staff_id
            join staff_profiles sp on sp.user_id = u.id
            join venues v on v.id = cs.venue_id
            where cs.venue_id = :venueId
              and cs.status = 'OPEN'
              and u.status = 'ACTIVE'
              and upper(sp.position) = 'CASHIER'
            order by cs.opened_at asc
            """, nativeQuery = true)
    List<OnDutyCashierProjection> findOnDutyCashiers(@Param("venueId") Long venueId);

    interface OnDutyCashierProjection {
        Long getStaffId();
        String getEmployeeCode();
        String getFullName();
        String getEmail();
        String getPhoneNumber();
        String getPosition();
        Long getShiftId();
        OffsetDateTime getOpenedAt();
        String getShiftStatus();
        Long getVenueId();
        String getVenueName();
    }
}
