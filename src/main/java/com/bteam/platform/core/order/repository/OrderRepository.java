package com.bteam.platform.core.order.repository;

import com.bteam.platform.core.order.model.Order;
import com.bteam.platform.core.order.model.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByOrderCode(String orderCode);

    @EntityGraph(attributePaths = {"venue", "customer", "items", "items.product"})
    Optional<Order> findWithItemsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"venue", "customer", "items", "items.product"})
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"venue", "customer", "items", "items.product"})
    @Query("""
            select distinct o
            from Order o
            where (:status is null or o.status = :status)
              and (:venueId is null or o.venue.id = :venueId)
            order by o.createdAt desc
            """)
    List<Order> findForManagement(
            @Param("status") OrderStatus status,
            @Param("venueId") Long venueId
    );
}
