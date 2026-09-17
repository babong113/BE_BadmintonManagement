package com.bteam.platform.core.court.service;

import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.court.dto.CourtAvailabilityResponse;
import com.bteam.platform.core.court.dto.CourtRequest;
import com.bteam.platform.core.court.dto.CourtResponse;
import com.bteam.platform.core.court.dto.CourtStatusResponse;
import com.bteam.platform.core.court.repository.CourtManagementRepository;
import com.bteam.platform.core.venue.model.Court;
import com.bteam.platform.core.venue.model.CourtStatus;
import com.bteam.platform.core.venue.model.Venue;
import com.bteam.platform.core.venue.model.VenueStatus;
import com.bteam.platform.core.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourtService {
    private final CourtManagementRepository courtRepository;
    private final VenueRepository venueRepository;

    @Transactional(readOnly = true)
    public List<CourtResponse> getCourts() {
        return courtRepository.findAllByOrderByVenueNameAscCourtCodeAsc()
                .stream()
                .map(this::toCourtResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourtResponse> getCourtsByVenue(Long venueId) {
        ensureActiveVenue(venueId);

        return courtRepository.findByVenueIdOrderByCourtCodeAsc(venueId)
                .stream()
                .map(this::toCourtResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourtResponse getCourtDetail(Long id) {
        return toCourtResponse(findCourt(id));
    }

    @Transactional(readOnly = true)
    public CourtStatusResponse getCourtStatus(Long id) {
        Court court = findCourt(id);
        return new CourtStatusResponse(court.getId(), court.getCourtCode(), court.getName(), court.getStatus());
    }

    @Transactional
    public CourtResponse createCourt(CourtRequest request) {
        Venue venue = ensureActiveVenue(request.venueId());
        validateCourtRequest(request);
        ensureCourtCodeAvailable(venue.getId(), request.courtCode(), null);

        Court court = Court.builder()
                .venue(venue)
                .courtCode(request.courtCode().trim())
                .name(request.name().trim())
                .description(trimToNull(request.description()))
                .pricePerHour(request.pricePerHour())
                .status(request.status() == null ? CourtStatus.AVAILABLE : request.status())
                .imageUrl(trimToNull(request.imageUrl()))
                .build();

        return toCourtResponse(courtRepository.save(court));
    }

    @Transactional
    public CourtResponse updateCourt(Long id, CourtRequest request) {
        Court court = findCourt(id);
        Venue venue = ensureActiveVenue(request.venueId());
        validateCourtRequest(request);
        ensureCourtCodeAvailable(venue.getId(), request.courtCode(), court.getId());

        court.setVenue(venue);
        court.setCourtCode(request.courtCode().trim());
        court.setName(request.name().trim());
        court.setDescription(trimToNull(request.description()));
        court.setPricePerHour(request.pricePerHour());
        court.setStatus(request.status() == null ? court.getStatus() : request.status());
        court.setImageUrl(trimToNull(request.imageUrl()));

        return toCourtResponse(courtRepository.save(court));
    }

    @Transactional
    public CourtResponse deactivateCourt(Long id) {
        return updateStatus(id, CourtStatus.INACTIVE);
    }

    @Transactional
    public CourtResponse markMaintenance(Long id) {
        return updateStatus(id, CourtStatus.MAINTENANCE);
    }

    @Transactional
    public CourtResponse reopenCourt(Long id) {
        return updateStatus(id, CourtStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public List<CourtResponse> searchCourts(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() < 2) {
            throw new InvalidDataException("Tu khoa tim kiem phai co it nhat 2 ky tu");
        }

        return courtRepository.searchCourts(normalizedKeyword)
                .stream()
                .map(this::toCourtResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourtResponse> filterByPrice(BigDecimal minPrice, BigDecimal maxPrice) {
        BigDecimal normalizedMin = minPrice == null ? BigDecimal.ZERO : minPrice;
        BigDecimal normalizedMax = maxPrice == null ? new BigDecimal("999999999999.99") : maxPrice;

        if (normalizedMin.compareTo(BigDecimal.ZERO) < 0 || normalizedMax.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidDataException("Gia loc phai lon hon hoac bang 0");
        }
        if (normalizedMax.compareTo(normalizedMin) < 0) {
            throw new InvalidDataException("Gia toi da phai lon hon hoac bang gia toi thieu");
        }

        return courtRepository.findByPricePerHourBetweenOrderByPricePerHourAsc(normalizedMin, normalizedMax)
                .stream()
                .map(this::toCourtResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourtAvailabilityResponse checkAvailability(
            Long id,
            LocalDate bookingDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        Court court = findCourt(id);

        if (bookingDate == null || startTime == null || endTime == null) {
            throw new InvalidDataException("Ngay dat san, gio bat dau va gio ket thuc khong duoc de trong");
        }
        if (!endTime.isAfter(startTime)) {
            throw new InvalidDataException("Gio ket thuc phai sau gio bat dau");
        }

        if (court.getStatus() != CourtStatus.AVAILABLE) {
            return new CourtAvailabilityResponse(
                    court.getId(),
                    court.getCourtCode(),
                    court.getName(),
                    court.getStatus(),
                    bookingDate,
                    startTime,
                    endTime,
                    false,
                    "San khong o trang thai AVAILABLE"
            );
        }

        long overlapCount = courtRepository.countOverlappingBookings(
                court.getId(),
                bookingDate,
                startTime,
                endTime
        );
        boolean available = overlapCount == 0;

        return new CourtAvailabilityResponse(
                court.getId(),
                court.getCourtCode(),
                court.getName(),
                court.getStatus(),
                bookingDate,
                startTime,
                endTime,
                available,
                available ? "San trong" : "San da co booking trung thoi gian"
        );
    }

    private CourtResponse updateStatus(Long id, CourtStatus status) {
        Court court = findCourt(id);
        court.setStatus(status);
        return toCourtResponse(courtRepository.save(court));
    }

    private Court findCourt(Long id) {
        return courtRepository.findById(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay san"));
    }

    private Venue ensureActiveVenue(Long venueId) {
        return venueRepository.findByIdAndStatus(venueId, VenueStatus.ACTIVE)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay co so dang hoat dong"));
    }

    private void validateCourtRequest(CourtRequest request) {
        if (request.pricePerHour() != null && request.pricePerHour().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidDataException("Gia thue phai lon hon hoac bang 0");
        }
    }

    private void ensureCourtCodeAvailable(Long venueId, String courtCode, Long currentCourtId) {
        courtRepository.findByVenueIdAndCourtCodeIgnoreCase(venueId, courtCode.trim())
                .filter(existingCourt -> currentCourtId == null || !existingCourt.getId().equals(currentCourtId))
                .ifPresent(existingCourt -> {
                    throw new InvalidDataException("Ma san da ton tai trong co so nay");
                });
    }

    private CourtResponse toCourtResponse(Court court) {
        return new CourtResponse(
                court.getId(),
                court.getVenue().getId(),
                court.getVenue().getName(),
                court.getCourtCode(),
                court.getName(),
                court.getDescription(),
                court.getPricePerHour(),
                court.getStatus(),
                court.getImageUrl()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
