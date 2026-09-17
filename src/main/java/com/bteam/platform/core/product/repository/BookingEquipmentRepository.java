package com.bteam.platform.core.product.repository;

import com.bteam.platform.core.product.model.BookingEquipment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingEquipmentRepository extends JpaRepository<BookingEquipment, Long> {
    @EntityGraph(attributePaths = {"product"})
    List<BookingEquipment> findByBookingDetailIdOrderByIdAsc(Long bookingDetailId);
}
