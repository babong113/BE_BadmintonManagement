package com.bteam.platform.core.venue.repository;

import com.bteam.platform.core.venue.model.Court;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourtRepository extends JpaRepository<Court, Long> {
    List<Court> findByVenueIdOrderByCourtCodeAsc(Long venueId);
}
