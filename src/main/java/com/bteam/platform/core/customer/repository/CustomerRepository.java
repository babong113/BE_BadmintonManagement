package com.bteam.platform.core.customer.repository;

import com.bteam.platform.adapter.persistence.jpa.entity.UserEntity;
import com.bteam.platform.core.auth.model.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    @Query("""
            select distinct u
            from UserEntity u
            join u.roles r
            where r.name = 'CUSTOMER'
              and (
                  lower(u.fullName) like lower(concat('%', :keyword, '%'))
                  or lower(u.email) like lower(concat('%', :keyword, '%'))
                  or u.phoneNumber like concat('%', :keyword, '%')
              )
            order by u.fullName asc
            """)
    List<UserEntity> searchCustomers(@Param("keyword") String keyword);

    @Query("""
            select distinct u
            from UserEntity u
            join u.roles r
            where u.id = :id and r.name = 'CUSTOMER'
            """)
    Optional<UserEntity> findCustomerById(@Param("id") Long id);

    interface BookingHistoryProjection {
        Long getId();
        String getBookingCode();
        LocalDate getBookingDate();
        BigDecimal getTotalAmount();
        String getStatus();
        String getNote();
        OffsetDateTime getCreatedAt();
    }

    interface PaymentHistoryProjection {
        Long getId();
        Long getBookingId();
        String getBookingCode();
        BigDecimal getAmount();
        String getPaymentMethod();
        String getPaymentStatus();
        String getTransactionCode();
        OffsetDateTime getPaidAt();
        OffsetDateTime getCreatedAt();
    }

    interface OrderHistoryProjection {
        Long getId();
        String getOrderCode();
        BigDecimal getTotalAmount();
        String getStatus();
        OffsetDateTime getCreatedAt();
    }

    @Query(value = """
            select
                b.id as id,
                b.booking_code as bookingCode,
                b.booking_date as bookingDate,
                b.total_amount as totalAmount,
                b.status as status,
                b.note as note,
                b.created_at as createdAt
            from bookings b
            where b.customer_id = :customerId
            order by b.booking_date desc, b.created_at desc
            """, nativeQuery = true)
    List<BookingHistoryProjection> findBookingHistory(@Param("customerId") Long customerId);

    @Query(value = """
            select
                p.id as id,
                b.id as bookingId,
                b.booking_code as bookingCode,
                p.amount as amount,
                p.payment_method as paymentMethod,
                p.payment_status as paymentStatus,
                p.transaction_code as transactionCode,
                p.paid_at as paidAt,
                p.created_at as createdAt
            from payments p
            join bookings b on b.id = p.booking_id
            where b.customer_id = :customerId
            order by p.created_at desc
            """, nativeQuery = true)
    List<PaymentHistoryProjection> findPaymentHistory(@Param("customerId") Long customerId);

    @Query(value = """
            select
                o.id as id,
                o.order_code as orderCode,
                o.total_amount as totalAmount,
                o.status as status,
                o.created_at as createdAt
            from orders o
            where o.customer_id = :customerId
            order by o.created_at desc
            """, nativeQuery = true)
    List<OrderHistoryProjection> findOrderHistory(@Param("customerId") Long customerId);

    long countByStatus(AccountStatus status);
}
