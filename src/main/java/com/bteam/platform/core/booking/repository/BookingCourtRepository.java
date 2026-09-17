package com.bteam.platform.core.booking.repository;

import com.bteam.platform.core.venue.model.Court;
import com.bteam.platform.core.venue.model.CourtStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingCourtRepository extends JpaRepository<Court, Long> {
    List<Court> findByStatusOrderByVenueNameAscCourtCodeAsc(CourtStatus status);
}
