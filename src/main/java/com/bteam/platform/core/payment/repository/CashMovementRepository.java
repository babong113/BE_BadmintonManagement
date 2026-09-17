package com.bteam.platform.core.payment.repository;

import com.bteam.platform.core.payment.model.CashMovement;
import com.bteam.platform.core.payment.model.CashMovementType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {
    @EntityGraph(attributePaths = "createdBy")
    List<CashMovement> findByCashierShiftIdOrderByCreatedAtAsc(Long cashierShiftId);

    @Query("""
            select coalesce(sum(cm.amount), 0)
            from CashMovement cm
            where cm.cashierShift.id = :shiftId and cm.movementType = :movementType
            """)
    BigDecimal sumAmountByShiftAndType(
            @Param("shiftId") Long shiftId,
            @Param("movementType") CashMovementType movementType
    );
}
