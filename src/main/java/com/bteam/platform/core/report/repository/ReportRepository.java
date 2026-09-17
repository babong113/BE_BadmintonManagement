package com.bteam.platform.core.report.repository;

import com.bteam.platform.core.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReportRepository extends JpaRepository<Booking, Long> {
    interface RevenueTotalsProjection {
        BigDecimal getTotalRevenue();
        BigDecimal getBookingRevenue();
        BigDecimal getOrderRevenue();
        Long getTransactionCount();
    }

    interface PeriodRevenueProjection {
        String getPeriod();
        BigDecimal getRevenue();
        Long getTransactionCount();
    }

    interface VenueRevenueProjection {
        Long getVenueId();
        String getVenueName();
        BigDecimal getRevenue();
    }

    interface CourtRevenueProjection {
        Long getCourtId();
        String getCourtName();
        Long getVenueId();
        String getVenueName();
        BigDecimal getRevenue();
    }

    interface StatusCountProjection {
        String getStatus();
        Long getTotal();
    }

    interface PeriodCountProjection {
        String getPeriod();
        Long getTotal();
    }

    interface CancellationProjection {
        Long getTotalBookings();
        Long getCancelledBookings();
    }

    interface PeakHourProjection {
        Integer getHour();
        Long getBookingCount();
    }

    interface PopularCourtProjection {
        Long getCourtId();
        String getCourtName();
        Long getVenueId();
        String getVenueName();
        Long getBookingCount();
    }

    interface CustomerActivityProjection {
        Long getCustomerId();
        String getCustomerName();
        String getEmail();
        Long getBookingCount();
        Long getCompletedBookingCount();
        LocalDate getLastBookingDate();
    }

    interface ProductPerformanceProjection {
        Long getProductId();
        String getProductName();
        Long getSoldQuantity();
        BigDecimal getSalesValue();
        Long getRentedQuantity();
        BigDecimal getRentalValue();
    }

    interface OrderTotalsProjection {
        Long getTotalOrders();
        BigDecimal getTotalValue();
        BigDecimal getCompletedValue();
    }

    interface ReviewPerformanceProjection {
        Long getVenueId();
        String getVenueName();
        Long getCourtId();
        String getCourtName();
        BigDecimal getAverageRating();
        Long getReviewCount();
    }

    interface DashboardCountsProjection {
        Long getActiveVenues();
        Long getActiveCourts();
        Long getTotalCustomers();
        BigDecimal getAverageRating();
    }

    @Query(value = """
            select
                coalesce(sum(p.amount), 0) as totalRevenue,
                coalesce(sum(case when p.booking_id is not null then p.amount else 0 end), 0) as bookingRevenue,
                coalesce(sum(case when p.order_id is not null then p.amount else 0 end), 0) as orderRevenue,
                count(p.id) as transactionCount
            from payments p
            where p.payment_status = 'PAID'
              and p.paid_at is not null
              and p.paid_at >= cast(:fromDate as date)
              and p.paid_at < cast(:toDate as date) + interval '1 day'
            """, nativeQuery = true)
    RevenueTotalsProjection findRevenueTotals(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select
                to_char(p.paid_at, 'YYYY-MM-DD') as period,
                coalesce(sum(p.amount), 0) as revenue,
                count(p.id) as transactionCount
            from payments p
            where p.payment_status = 'PAID'
              and p.paid_at is not null
              and p.paid_at >= cast(:fromDate as date)
              and p.paid_at < cast(:toDate as date) + interval '1 day'
            group by to_char(p.paid_at, 'YYYY-MM-DD')
            order by period
            """, nativeQuery = true)
    List<PeriodRevenueProjection> findDailyRevenue(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select
                to_char(p.paid_at, 'YYYY-MM') as period,
                coalesce(sum(p.amount), 0) as revenue,
                count(p.id) as transactionCount
            from payments p
            where p.payment_status = 'PAID'
              and p.paid_at is not null
              and p.paid_at >= cast(:fromDate as date)
              and p.paid_at < cast(:toDate as date) + interval '1 day'
            group by to_char(p.paid_at, 'YYYY-MM')
            order by period
            """, nativeQuery = true)
    List<PeriodRevenueProjection> findMonthlyRevenue(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select x.venue_id as venueId, x.venue_name as venueName,
                   coalesce(sum(x.revenue), 0) as revenue
            from (
                select v.id as venue_id, v.name as venue_name, sum(pa.amount) as revenue
                from payment_allocations pa
                join payments p on p.id = pa.payment_id
                join booking_details bd on bd.id = pa.booking_detail_id
                join courts c on c.id = bd.court_id
                join venues v on v.id = c.venue_id
                where p.payment_status = 'PAID'
                  and p.paid_at is not null
                  and p.paid_at >= cast(:fromDate as date)
                  and p.paid_at < cast(:toDate as date) + interval '1 day'
                group by v.id, v.name

                union all

                select v.id as venue_id, v.name as venue_name, sum(p.amount) as revenue
                from payments p
                join orders o on o.id = p.order_id
                join venues v on v.id = o.venue_id
                where p.payment_status = 'PAID'
                  and p.paid_at is not null
                  and p.paid_at >= cast(:fromDate as date)
                  and p.paid_at < cast(:toDate as date) + interval '1 day'
                group by v.id, v.name
            ) x
            group by x.venue_id, x.venue_name
            order by revenue desc, x.venue_name
            """, nativeQuery = true)
    List<VenueRevenueProjection> findRevenueByVenue(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select c.id as courtId, c.name as courtName,
                   v.id as venueId, v.name as venueName,
                   coalesce(sum(pa.amount), 0) as revenue
            from payment_allocations pa
            join payments p on p.id = pa.payment_id
            join booking_details bd on bd.id = pa.booking_detail_id
            join courts c on c.id = bd.court_id
            join venues v on v.id = c.venue_id
            where p.payment_status = 'PAID'
              and p.paid_at is not null
              and p.paid_at >= cast(:fromDate as date)
              and p.paid_at < cast(:toDate as date) + interval '1 day'
            group by c.id, c.name, v.id, v.name
            order by revenue desc, c.name
            """, nativeQuery = true)
    List<CourtRevenueProjection> findRevenueByCourt(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select b.status as status, count(*) as total
            from bookings b
            where b.booking_date between :fromDate and :toDate
            group by b.status
            order by b.status
            """, nativeQuery = true)
    List<StatusCountProjection> findBookingStatusCounts(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select to_char(b.booking_date, 'YYYY-MM-DD') as period, count(*) as total
            from bookings b
            where b.booking_date between :fromDate and :toDate
            group by b.booking_date
            order by b.booking_date
            """, nativeQuery = true)
    List<PeriodCountProjection> findDailyBookingCounts(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select to_char(b.booking_date, 'YYYY-MM') as period, count(*) as total
            from bookings b
            where b.booking_date between :fromDate and :toDate
            group by to_char(b.booking_date, 'YYYY-MM')
            order by period
            """, nativeQuery = true)
    List<PeriodCountProjection> findMonthlyBookingCounts(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select count(*) as totalBookings,
                   count(*) filter (where b.status = 'CANCELLED') as cancelledBookings
            from bookings b
            where b.booking_date between :fromDate and :toDate
            """, nativeQuery = true)
    CancellationProjection findCancellationCounts(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select cast(extract(hour from bd.start_time) as integer) as hour,
                   count(*) as bookingCount
            from booking_details bd
            join bookings b on b.id = bd.booking_id
            where b.booking_date between :fromDate and :toDate
              and b.status <> 'CANCELLED'
              and bd.status <> 'CANCELLED'
            group by extract(hour from bd.start_time)
            order by bookingCount desc, hour
            """, nativeQuery = true)
    List<PeakHourProjection> findPeakHours(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select c.id as courtId, c.name as courtName,
                   v.id as venueId, v.name as venueName,
                   count(*) as bookingCount
            from booking_details bd
            join bookings b on b.id = bd.booking_id
            join courts c on c.id = bd.court_id
            join venues v on v.id = c.venue_id
            where b.booking_date between :fromDate and :toDate
              and b.status <> 'CANCELLED'
              and bd.status <> 'CANCELLED'
            group by c.id, c.name, v.id, v.name
            order by bookingCount desc, c.name
            """, nativeQuery = true)
    List<PopularCourtProjection> findPopularCourts(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select u.id as customerId, u.full_name as customerName, u.email as email,
                   count(b.id) filter (where b.booking_date between :fromDate and :toDate) as bookingCount,
                   count(b.id) filter (where b.booking_date between :fromDate and :toDate and b.status = 'COMPLETED') as completedBookingCount,
                   max(b.booking_date) filter (where b.booking_date between :fromDate and :toDate) as lastBookingDate
            from users u
            join user_roles ur on ur.user_id = u.id
            join roles r on r.id = ur.role_id and r.name = 'CUSTOMER'
            left join bookings b on b.customer_id = u.id
            group by u.id, u.full_name, u.email
            order by bookingCount desc, u.full_name
            """, nativeQuery = true)
    List<CustomerActivityProjection> findCustomerActivity(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select e.id as productId, e.name as productName,
                   coalesce(s.sold_quantity, 0) as soldQuantity,
                   coalesce(s.sales_value, 0) as salesValue,
                   coalesce(r.rented_quantity, 0) as rentedQuantity,
                   coalesce(r.rental_value, 0) as rentalValue
            from equipment e
            left join (
                select oi.equipment_id,
                       sum(oi.quantity) as sold_quantity,
                       sum(oi.subtotal) as sales_value
                from order_items oi
                join orders o on o.id = oi.order_id
                where o.status = 'COMPLETED'
                  and o.created_at >= cast(:fromDate as date)
                  and o.created_at < cast(:toDate as date) + interval '1 day'
                group by oi.equipment_id
            ) s on s.equipment_id = e.id
            left join (
                select be.equipment_id,
                       sum(be.quantity) as rented_quantity,
                       sum(be.subtotal) as rental_value
                from booking_equipment be
                join booking_details bd on bd.id = be.booking_detail_id
                join bookings b on b.id = bd.booking_id
                where bd.status = 'COMPLETED'
                  and b.booking_date between :fromDate and :toDate
                group by be.equipment_id
            ) r on r.equipment_id = e.id
            where coalesce(s.sold_quantity, 0) > 0 or coalesce(r.rented_quantity, 0) > 0
            order by (coalesce(s.sold_quantity, 0) + coalesce(r.rented_quantity, 0)) desc, e.name
            """, nativeQuery = true)
    List<ProductPerformanceProjection> findProductPerformance(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select count(*) as totalOrders,
                   coalesce(sum(o.total_amount), 0) as totalValue,
                   coalesce(sum(case when o.status = 'COMPLETED' then o.total_amount else 0 end), 0) as completedValue
            from orders o
            where o.created_at >= cast(:fromDate as date)
              and o.created_at < cast(:toDate as date) + interval '1 day'
            """, nativeQuery = true)
    OrderTotalsProjection findOrderTotals(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select o.status as status, count(*) as total
            from orders o
            where o.created_at >= cast(:fromDate as date)
              and o.created_at < cast(:toDate as date) + interval '1 day'
            group by o.status
            order by o.status
            """, nativeQuery = true)
    List<StatusCountProjection> findOrderStatusCounts(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select v.id as venueId, v.name as venueName,
                   c.id as courtId, c.name as courtName,
                   cast(avg(rv.rating) as numeric(4,2)) as averageRating,
                   count(rv.id) as reviewCount
            from reviews rv
            join booking_details bd on bd.id = rv.booking_detail_id
            join courts c on c.id = bd.court_id
            join venues v on v.id = c.venue_id
            where rv.created_at >= cast(:fromDate as date)
              and rv.created_at < cast(:toDate as date) + interval '1 day'
            group by v.id, v.name, c.id, c.name
            order by averageRating desc, reviewCount desc, c.name
            """, nativeQuery = true)
    List<ReviewPerformanceProjection> findReviewPerformance(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(value = """
            select
                (select count(*) from venues where status = 'ACTIVE') as activeVenues,
                (select count(*) from courts where status = 'AVAILABLE') as activeCourts,
                (select count(distinct u.id)
                 from users u
                 join user_roles ur on ur.user_id = u.id
                 join roles r on r.id = ur.role_id
                 where r.name = 'CUSTOMER') as totalCustomers,
                coalesce((select cast(avg(rv.rating) as numeric(4,2))
                          from reviews rv
                          where rv.created_at >= cast(:fromDate as date)
                            and rv.created_at < cast(:toDate as date) + interval '1 day'), 0) as averageRating
            """, nativeQuery = true)
    DashboardCountsProjection findDashboardCounts(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
