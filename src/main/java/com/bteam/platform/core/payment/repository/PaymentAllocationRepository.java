package com.bteam.platform.core.payment.repository;

import com.bteam.platform.core.payment.model.PaymentAllocation;
import com.bteam.platform.core.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {
    @Query("""
            select coalesce(sum(a.amount), 0)
            from PaymentAllocation a
            where a.bookingDetail.id = :bookingDetailId
              and a.payment.paymentStatus in :statuses
            """)
    BigDecimal sumActiveAmountByBookingDetail(
            @Param("bookingDetailId") Long bookingDetailId,
            @Param("statuses") Collection<PaymentStatus> statuses
    );
}
