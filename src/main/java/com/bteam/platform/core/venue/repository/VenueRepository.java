package com.bteam.platform.core.venue.repository;

import com.bteam.platform.core.venue.model.Venue;
import com.bteam.platform.core.venue.model.VenueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    List<Venue> findByStatusOrderByNameAsc(VenueStatus status);

    Optional<Venue> findByIdAndStatus(Long id, VenueStatus status);

    List<Venue> findByStatusAndLatitudeIsNotNullAndLongitudeIsNotNull(VenueStatus status);
}
