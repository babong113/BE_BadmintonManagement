package com.bteam.platform.core.review.service;

import com.bteam.platform.core.booking.model.BookingDetail;
import com.bteam.platform.core.booking.model.BookingDetailStatus;
import com.bteam.platform.core.booking.repository.BookingDetailRepository;
import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.review.dto.CourtRatingSummaryResponse;
import com.bteam.platform.core.review.dto.CreateReviewRequest;
import com.bteam.platform.core.review.dto.ReviewResponse;
import com.bteam.platform.core.review.model.Review;
import com.bteam.platform.core.review.repository.ReviewRepository;
import com.bteam.platform.core.venue.model.Court;
import com.bteam.platform.core.venue.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final CourtRepository courtRepository;

    @Transactional(readOnly = true)
    public List<ReviewResponse> getCourtReviews(Long courtId) {
        ensureCourtExists(courtId);
        return reviewRepository.findByBookingDetailCourtIdOrderByCreatedAtDesc(courtId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourtRatingSummaryResponse getCourtRatingSummary(Long courtId) {
        Court court = ensureCourtExists(courtId);
        Double average = reviewRepository.findAverageRatingByCourtId(courtId);
        BigDecimal averageRating = average == null
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
        return new CourtRatingSummaryResponse(
                court.getId(),
                court.getName(),
                averageRating,
                reviewRepository.countByBookingDetailCourtId(courtId)
        );
    }

    @Transactional
    public ReviewResponse createReview(
            Long bookingDetailId,
            CreateReviewRequest request,
            Authentication authentication
    ) {
        BookingDetail detail = bookingDetailRepository.findByIdForUpdate(bookingDetailId)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay booking detail"));
        if (detail.getBooking().getCustomer() == null
                || !detail.getBooking().getCustomer().getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new InvalidDataException("Ban khong co quyen danh gia booking detail nay");
        }
        if (detail.getStatus() != BookingDetailStatus.COMPLETED) {
            throw new InvalidDataException("Chi co the danh gia booking detail da hoan tat");
        }
        if (reviewRepository.existsByBookingDetailId(bookingDetailId)) {
            throw new InvalidDataException("Booking detail da duoc danh gia");
        }

        Review review = Review.builder()
                .bookingDetail(detail)
                .customer(detail.getBooking().getCustomer())
                .rating(request.rating())
                .comment(trimToNull(request.comment()))
                .build();
        return toResponse(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse moderateReview(Long reviewId) {
        Review review = reviewRepository.findWithDetailsById(reviewId)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay review"));
        review.setComment(null);
        return toResponse(reviewRepository.save(review));
    }

    private Court ensureCourtExists(Long courtId) {
        return courtRepository.findById(courtId)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay san"));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBookingDetail().getId(),
                review.getBookingDetail().getCourt().getId(),
                review.getBookingDetail().getCourt().getName(),
                review.getCustomer().getId(),
                review.getCustomer().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
