package com.bteam.platform.core.review.service;

import com.bteam.platform.adapter.persistence.jpa.entity.UserEntity;
import com.bteam.platform.core.booking.model.Booking;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookingDetailRepository bookingDetailRepository;
    @Mock
    private CourtRepository courtRepository;

    private ReviewService reviewService;
    private UserEntity customer;
    private BookingDetail detail;
    private Court court;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, bookingDetailRepository, courtRepository);

        customer = new UserEntity();
        customer.setId(10L);
        customer.setEmail("customer@example.com");
        customer.setFullName("Customer One");

        court = Court.builder().id(3L).name("Court A").build();
        Booking booking = Booking.builder().id(1L).customer(customer).build();
        detail = BookingDetail.builder()
                .id(2L)
                .booking(booking)
                .court(court)
                .status(BookingDetailStatus.COMPLETED)
                .build();
    }

    @Test
    void customerCanReviewOwnCompletedBookingDetail() {
        when(bookingDetailRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(detail));
        when(reviewRepository.existsByBookingDetailId(2L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(20L);
            return review;
        });

        ReviewResponse response = reviewService.createReview(
                2L,
                new CreateReviewRequest(5, "  San rat tot  "),
                customerAuthentication()
        );

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("San rat tot");
        assertThat(response.customerId()).isEqualTo(10L);
        assertThat(response.courtId()).isEqualTo(3L);
    }

    @Test
    void customerCannotReviewAnotherCustomersBookingDetail() {
        when(bookingDetailRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(detail));

        assertThatThrownBy(() -> reviewService.createReview(
                2L,
                new CreateReviewRequest(4, "Good"),
                authentication("other@example.com")
        ))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Ban khong co quyen danh gia booking detail nay");
    }

    @Test
    void customerCannotReviewBeforeDetailIsCompleted() {
        detail.setStatus(BookingDetailStatus.ACTIVE);
        when(bookingDetailRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(detail));

        assertThatThrownBy(() -> reviewService.createReview(
                2L,
                new CreateReviewRequest(4, null),
                customerAuthentication()
        ))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Chi co the danh gia booking detail da hoan tat");
    }

    @Test
    void bookingDetailCannotBeReviewedTwice() {
        when(bookingDetailRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(detail));
        when(reviewRepository.existsByBookingDetailId(2L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(
                2L,
                new CreateReviewRequest(4, null),
                customerAuthentication()
        ))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Booking detail da duoc danh gia");
    }

    @Test
    void courtSummaryRoundsAverageToTwoDecimalPlaces() {
        when(courtRepository.findById(3L)).thenReturn(Optional.of(court));
        when(reviewRepository.findAverageRatingByCourtId(3L)).thenReturn(4.333333);
        when(reviewRepository.countByBookingDetailCourtId(3L)).thenReturn(3L);

        CourtRatingSummaryResponse response = reviewService.getCourtRatingSummary(3L);

        assertThat(response.averageRating()).isEqualByComparingTo(new BigDecimal("4.33"));
        assertThat(response.reviewCount()).isEqualTo(3L);
    }

    @Test
    void ownerCanRemoveViolatingCommentWithoutDeletingReview() {
        Review review = Review.builder()
                .id(20L)
                .bookingDetail(detail)
                .customer(customer)
                .rating(1)
                .comment("Violating content")
                .build();
        when(reviewRepository.findWithDetailsById(20L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);

        ReviewResponse response = reviewService.moderateReview(20L);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.comment()).isNull();
        assertThat(response.rating()).isEqualTo(1);
    }

    private Authentication customerAuthentication() {
        return authentication("customer@example.com");
    }

    private Authentication authentication(String email) {
        return new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("review:create"))
        );
    }
}
