package com.bteam.platform.core.product.repository;

import com.bteam.platform.core.product.model.Product;
import com.bteam.platform.core.product.model.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStatusAndQuantityGreaterThanOrderByNameAsc(ProductStatus status, Integer quantity);

    List<Product> findByStatusAndQuantityGreaterThanAndNameContainingIgnoreCaseOrderByNameAsc(
            ProductStatus status,
            Integer quantity,
            String name
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
