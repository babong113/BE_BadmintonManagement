package com.bteam.platform.core.review.controller;

import com.bteam.platform.core.common.response.ApiResponse;
import com.bteam.platform.core.review.dto.CourtRatingSummaryResponse;
import com.bteam.platform.core.review.dto.CreateReviewRequest;
import com.bteam.platform.core.review.dto.ReviewResponse;
import com.bteam.platform.core.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping("/courts/{courtId}")
    @PreAuthorize("hasAuthority('review:read')")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getCourtReviews(@PathVariable Long courtId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay danh sach danh gia thanh cong",
                reviewService.getCourtReviews(courtId)
        ));
    }

    @GetMapping("/courts/{courtId}/summary")
    @PreAuthorize("hasAuthority('review:read')")
    public ResponseEntity<ApiResponse<CourtRatingSummaryResponse>> getCourtRatingSummary(
            @PathVariable Long courtId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay diem danh gia trung binh thanh cong",
                reviewService.getCourtRatingSummary(courtId)
        ));
    }

    @PostMapping("/booking-details/{bookingDetailId}")
    @PreAuthorize("hasAuthority('review:create')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long bookingDetailId,
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Danh gia san thanh cong",
                reviewService.createReview(bookingDetailId, request, authentication)
        ));
    }

    @PatchMapping("/{reviewId}/moderate")
    @PreAuthorize("hasAuthority('review:manage')")
    public ResponseEntity<ApiResponse<ReviewResponse>> moderateReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Go noi dung review vi pham thanh cong",
                reviewService.moderateReview(reviewId)
        ));
    }
}
