package com.bteam.platform.core.review.repository;

import com.bteam.platform.core.review.model.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByBookingDetailId(Long bookingDetailId);

    @EntityGraph(attributePaths = {"customer", "bookingDetail", "bookingDetail.court"})
    List<Review> findByBookingDetailCourtIdOrderByCreatedAtDesc(Long courtId);

    @EntityGraph(attributePaths = {"customer", "bookingDetail", "bookingDetail.court"})
    @Query("select r from Review r where r.id = :id")
    Optional<Review> findWithDetailsById(@Param("id") Long id);

    @Query("select avg(r.rating) from Review r where r.bookingDetail.court.id = :courtId")
    Double findAverageRatingByCourtId(@Param("courtId") Long courtId);

    long countByBookingDetailCourtId(Long courtId);
}
