package com.bteam.platform.core.payment.repository;

import com.bteam.platform.core.payment.model.Payment;
import com.bteam.platform.core.payment.model.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @EntityGraph(attributePaths = {"booking", "booking.customer", "order", "order.customer", "cashierShift", "receivedBy", "allocations", "allocations.bookingDetail"})
    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    @EntityGraph(attributePaths = {"booking", "booking.customer", "order", "order.customer", "cashierShift", "receivedBy", "allocations", "allocations.bookingDetail"})
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findWithDetailsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p left join fetch p.booking left join fetch p.order where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"order", "order.customer", "cashierShift", "receivedBy"})
    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from Payment p
            where p.booking.id = :bookingId and p.paymentStatus in :statuses
            """)
    BigDecimal sumAmountByBookingAndStatuses(
            @Param("bookingId") Long bookingId,
            @Param("statuses") Collection<PaymentStatus> statuses
    );

    @Query("""
            select coalesce(sum(p.amount), 0)
            from Payment p
            where p.order.id = :orderId and p.paymentStatus in :statuses
            """)
    BigDecimal sumAmountByOrderAndStatuses(
            @Param("orderId") Long orderId,
            @Param("statuses") Collection<PaymentStatus> statuses
    );
}
