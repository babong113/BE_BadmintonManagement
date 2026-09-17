package com.bteam.platform.core.venue.service;

import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.venue.dto.CourtResponse;
import com.bteam.platform.core.venue.dto.NearbyVenueResponse;
import com.bteam.platform.core.venue.dto.VenueLocationResponse;
import com.bteam.platform.core.venue.dto.VenueRequest;
import com.bteam.platform.core.venue.dto.VenueResponse;
import com.bteam.platform.core.venue.model.Court;
import com.bteam.platform.core.venue.model.Venue;
import com.bteam.platform.core.venue.model.VenueStatus;
import com.bteam.platform.core.venue.repository.CourtRepository;
import com.bteam.platform.core.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueService {
    private static final int DEFAULT_NEARBY_LIMIT = 5;
    private static final int MAX_NEARBY_LIMIT = 50;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;

    @Transactional(readOnly = true)
    public List<VenueResponse> getActiveVenues() {
        return venueRepository.findByStatusOrderByNameAsc(VenueStatus.ACTIVE)
                .stream()
                .map(this::toVenueResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VenueResponse getVenueDetail(Long id) {
        return toVenueResponse(getActiveVenue(id));
    }

    @Transactional(readOnly = true)
    public VenueLocationResponse getVenueLocation(Long id) {
        Venue venue = getActiveVenue(id);
        return new VenueLocationResponse(
                venue.getId(),
                venue.getName(),
                venue.getAddress(),
                venue.getLatitude(),
                venue.getLongitude()
        );
    }

    @Transactional
    public VenueResponse createVenue(VenueRequest request) {
        validateOperatingHours(request);

        Venue venue = Venue.builder()
                .name(request.name().trim())
                .address(request.address().trim())
                .phoneNumber(trimToNull(request.phoneNumber()))
                .latitude(request.latitude())
                .longitude(request.longitude())
                .openingTime(request.openingTime())
                .closingTime(request.closingTime())
                .status(VenueStatus.ACTIVE)
                .build();

        return toVenueResponse(venueRepository.save(venue));
    }

    @Transactional
    public VenueResponse updateVenue(Long id, VenueRequest request) {
        validateOperatingHours(request);

        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay co so"));

        venue.setName(request.name().trim());
        venue.setAddress(request.address().trim());
        venue.setPhoneNumber(trimToNull(request.phoneNumber()));
        venue.setLatitude(request.latitude());
        venue.setLongitude(request.longitude());
        venue.setOpeningTime(request.openingTime());
        venue.setClosingTime(request.closingTime());

        return toVenueResponse(venueRepository.save(venue));
    }

    @Transactional
    public VenueResponse deactivateVenue(Long id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay co so"));

        venue.setStatus(VenueStatus.INACTIVE);
        return toVenueResponse(venueRepository.save(venue));
    }

    @Transactional(readOnly = true)
    public List<NearbyVenueResponse> getNearbyVenues(BigDecimal latitude, BigDecimal longitude, Integer limit) {
        int resultLimit = normalizeLimit(limit);
        double lat = latitude.doubleValue();
        double lon = longitude.doubleValue();

        return venueRepository.findByStatusAndLatitudeIsNotNullAndLongitudeIsNotNull(VenueStatus.ACTIVE)
                .stream()
                .map(venue -> toNearbyVenueResponse(venue, distanceKm(
                        lat,
                        lon,
                        venue.getLatitude().doubleValue(),
                        venue.getLongitude().doubleValue()
                )))
                .sorted(Comparator.comparingDouble(NearbyVenueResponse::distanceKm))
                .limit(resultLimit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourtResponse> getCourtsByVenue(Long venueId) {
        getActiveVenue(venueId);

        return courtRepository.findByVenueIdOrderByCourtCodeAsc(venueId)
                .stream()
                .map(this::toCourtResponse)
                .toList();
    }

    private Venue getActiveVenue(Long id) {
        return venueRepository.findByIdAndStatus(id, VenueStatus.ACTIVE)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay co so dang hoat dong"));
    }

    private void validateOperatingHours(VenueRequest request) {
        if (request.openingTime() != null
                && request.closingTime() != null
                && !request.closingTime().isAfter(request.openingTime())) {
            throw new InvalidDataException("Gio dong cua phai sau gio mo cua");
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_NEARBY_LIMIT;
        }
        if (limit < 1 || limit > MAX_NEARBY_LIMIT) {
            throw new InvalidDataException("Limit phai nam trong khoang 1 den 50");
        }
        return limit;
    }

    private VenueResponse toVenueResponse(Venue venue) {
        return new VenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getAddress(),
                venue.getPhoneNumber(),
                venue.getLatitude(),
                venue.getLongitude(),
                venue.getOpeningTime(),
                venue.getClosingTime(),
                venue.getStatus()
        );
    }

    private NearbyVenueResponse toNearbyVenueResponse(Venue venue, double distanceKm) {
        return new NearbyVenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getAddress(),
                venue.getPhoneNumber(),
                venue.getLatitude(),
                venue.getLongitude(),
                venue.getOpeningTime(),
                venue.getClosingTime(),
                Math.round(distanceKm * 100.0) / 100.0
        );
    }

    private CourtResponse toCourtResponse(Court court) {
        return new CourtResponse(
                court.getId(),
                court.getVenue().getId(),
                court.getCourtCode(),
                court.getName(),
                court.getDescription(),
                court.getPricePerHour(),
                court.getStatus(),
                court.getImageUrl()
        );
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
