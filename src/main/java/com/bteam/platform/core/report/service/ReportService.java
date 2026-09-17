package com.bteam.platform.core.report.service;

import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.report.dto.BookingReportResponse;
import com.bteam.platform.core.report.dto.CancellationRateResponse;
import com.bteam.platform.core.report.dto.CourtRevenueResponse;
import com.bteam.platform.core.report.dto.CustomerActivityResponse;
import com.bteam.platform.core.report.dto.CustomerReportResponse;
import com.bteam.platform.core.report.dto.DashboardResponse;
import com.bteam.platform.core.report.dto.OrderReportResponse;
import com.bteam.platform.core.report.dto.PeakHourResponse;
import com.bteam.platform.core.report.dto.PeriodCountResponse;
import com.bteam.platform.core.report.dto.PeriodRevenueResponse;
import com.bteam.platform.core.report.dto.PopularCourtResponse;
import com.bteam.platform.core.report.dto.ProductPerformanceResponse;
import com.bteam.platform.core.report.dto.RevenueSummaryResponse;
import com.bteam.platform.core.report.dto.ReviewPerformanceResponse;
import com.bteam.platform.core.report.dto.StatusCountResponse;
import com.bteam.platform.core.report.dto.VenueRevenueResponse;
import com.bteam.platform.core.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final ReportRepository reportRepository;

    public DashboardResponse getDashboard(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        ReportRepository.RevenueTotalsProjection revenue =
                reportRepository.findRevenueTotals(range.fromDate(), range.toDate());
        List<ReportRepository.StatusCountProjection> bookingStatuses =
                reportRepository.findBookingStatusCounts(range.fromDate(), range.toDate());
        List<ReportRepository.StatusCountProjection> orderStatuses =
                reportRepository.findOrderStatusCounts(range.fromDate(), range.toDate());
        ReportRepository.CancellationProjection cancellation =
                reportRepository.findCancellationCounts(range.fromDate(), range.toDate());
        ReportRepository.OrderTotalsProjection orders =
                reportRepository.findOrderTotals(range.fromDate(), range.toDate());
        ReportRepository.DashboardCountsProjection counts =
                reportRepository.findDashboardCounts(range.fromDate(), range.toDate());

        Map<String, Long> bookingStatusMap = toStatusMap(bookingStatuses);
        Map<String, Long> orderStatusMap = toStatusMap(orderStatuses);
        long totalBookings = longValue(cancellation.getTotalBookings());
        long cancelledBookings = longValue(cancellation.getCancelledBookings());

        return new DashboardResponse(
                range.fromDate(),
                range.toDate(),
                money(revenue.getTotalRevenue()),
                money(revenue.getBookingRevenue()),
                money(revenue.getOrderRevenue()),
                totalBookings,
                bookingStatusMap.getOrDefault("COMPLETED", 0L),
                cancelledBookings,
                percentage(cancelledBookings, totalBookings),
                longValue(orders.getTotalOrders()),
                orderStatusMap.getOrDefault("COMPLETED", 0L),
                longValue(counts.getTotalCustomers()),
                longValue(counts.getActiveVenues()),
                longValue(counts.getActiveCourts()),
                money(counts.getAverageRating())
        );
    }

    public RevenueSummaryResponse getRevenueSummary(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        ReportRepository.RevenueTotalsProjection projection =
                reportRepository.findRevenueTotals(range.fromDate(), range.toDate());
        return new RevenueSummaryResponse(
                range.fromDate(), range.toDate(),
                money(projection.getTotalRevenue()),
                money(projection.getBookingRevenue()),
                money(projection.getOrderRevenue()),
                longValue(projection.getTransactionCount())
        );
    }

    public List<PeriodRevenueResponse> getDailyRevenue(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        return reportRepository.findDailyRevenue(range.fromDate(), range.toDate()).stream()
                .map(item -> new PeriodRevenueResponse(
                        item.getPeriod(), money(item.getRevenue()), longValue(item.getTransactionCount())
                ))
                .toList();
    }

    public List<PeriodRevenueResponse> getMonthlyRevenue(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        return reportRepository.findMonthlyRevenue(range.fromDate(), range.toDate()).stream()
                .map(item -> new PeriodRevenueResponse(
                        item.getPeriod(), money(item.getRevenue()), longValue(item.getTransactionCount())
                ))
                .toList();
    }

    public List<VenueRevenueResponse> getRevenueByVenue(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        return reportRepository.findRevenueByVenue(range.fromDate(), range.toDate()).stream()
                .map(item -> new VenueRevenueResponse(
                        item.getVenueId(), item.getVenueName(), money(item.getRevenue())
                ))
                .toList();
    }

    public List<CourtRevenueResponse> getRevenueByCourt(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        return reportRepository.findRevenueByCourt(range.fromDate(), range.toDate()).stream()
                .map(item -> new CourtRevenueResponse(
                        item.getCourtId(), item.getCourtName(),
                        item.getVenueId(), item.getVenueName(), money(item.getRevenue())
                ))
                .toList();
    }

    public BookingReportResponse getBookingReport(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        List<StatusCountResponse> statuses = reportRepository
                .findBookingStatusCounts(range.fromDate(), range.toDate()).stream()
                .map(item -> new StatusCountResponse(item.getStatus(), longValue(item.getTotal())))
                .toList();
        List<PeriodCountResponse> daily = reportRepository
                .findDailyBookingCounts(range.fromDate(), range.toDate()).stream()
                .map(item -> new PeriodCountResponse(item.getPeriod(), longValue(item.getTotal())))
                .toList();
        List<PeriodCountResponse> monthly = reportRepository
                .findMonthlyBookingCounts(range.fromDate(), range.toDate()).stream()
                .map(item -> new PeriodCountResponse(item.getPeriod(), longValue(item.getTotal())))
                .toList();
        long total = statuses.stream().mapToLong(StatusCountResponse::total).sum();
        return new BookingReportResponse(
                range.fromDate(), range.toDate(), total, statuses, daily, monthly
        );
    }

    public CancellationRateResponse getCancellationRate(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        ReportRepository.CancellationProjection projection =
                reportRepository.findCancellationCounts(range.fromDate(), range.toDate());
        long total = longValue(projection.getTotalBookings());
        long cancelled = longValue(projection.getCancelledBookings());
        return new CancellationRateResponse(
                range.fromDate(), range.toDate(), total, cancelled, percentage(cancelled, total)
        );
    }

    public List<PeakHourResponse> getPeakHours(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        return reportRepository.findPeakHours(range.fromDate(), range.toDate()).stream()
                .map(item -> new PeakHourResponse(
                        item.getHour(),
                        String.format("%02d:00-%02d:00", item.getHour(), (item.getHour() + 1) % 24),
                        longValue(item.getBookingCount())
                ))
                .toList();
    }

    public List<PopularCourtResponse> getPopularCourts(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        return reportRepository.findPopularCourts(range.fromDate(), range.toDate()).stream()
                .map(item -> new PopularCourtResponse(
                        item.getCourtId(), item.getCourtName(), item.getVenueId(), item.getVenueName(),
                        longValue(item.getBookingCount())
                ))
                .toList();
    }

    public CustomerReportResponse getCustomerReport(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        List<CustomerActivityResponse> customers = reportRepository
                .findCustomerActivity(range.fromDate(), range.toDate()).stream()
                .map(item -> new CustomerActivityResponse(
                        item.getCustomerId(), item.getCustomerName(), item.getEmail(),
                        longValue(item.getBookingCount()), longValue(item.getCompletedBookingCount()),
                        item.getLastBookingDate()
                ))
                .toList();
        long totalBookings = customers.stream().mapToLong(CustomerActivityResponse::bookingCount).sum();
        BigDecimal average = customers.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(totalBookings)
                        .divide(BigDecimal.valueOf(customers.size()), 2, RoundingMode.HALF_UP);
        return new CustomerReportResponse(
                range.fromDate(), range.toDate(), customers.size(), average, customers
        );
    }

    public List<ProductPerformanceResponse> getProductReport(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        return reportRepository.findProductPerformance(range.fromDate(), range.toDate()).stream()
                .map(item -> {
                    long sold = longValue(item.getSoldQuantity());
                    long rented = longValue(item.getRentedQuantity());
                    return new ProductPerformanceResponse(
                            item.getProductId(), item.getProductName(), sold, money(item.getSalesValue()),
                            rented, money(item.getRentalValue()), sold + rented
                    );
                })
                .toList();
    }

    public OrderReportResponse getOrderReport(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        ReportRepository.OrderTotalsProjection totals =
                reportRepository.findOrderTotals(range.fromDate(), range.toDate());
        List<StatusCountResponse> statuses = reportRepository
                .findOrderStatusCounts(range.fromDate(), range.toDate()).stream()
                .map(item -> new StatusCountResponse(item.getStatus(), longValue(item.getTotal())))
                .toList();
        return new OrderReportResponse(
                range.fromDate(), range.toDate(), longValue(totals.getTotalOrders()),
                money(totals.getTotalValue()), money(totals.getCompletedValue()), statuses
        );
    }

    public List<ReviewPerformanceResponse> getReviewReport(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        return reportRepository.findReviewPerformance(range.fromDate(), range.toDate()).stream()
                .map(item -> new ReviewPerformanceResponse(
                        item.getVenueId(), item.getVenueName(), item.getCourtId(), item.getCourtName(),
                        money(item.getAverageRating()), longValue(item.getReviewCount())
                ))
                .toList();
    }

    private DateRange resolveRange(LocalDate fromDate, LocalDate toDate) {
        LocalDate end = toDate == null ? LocalDate.now() : toDate;
        LocalDate start = fromDate == null ? end.withDayOfMonth(1) : fromDate;
        if (start.isAfter(end)) {
            throw new InvalidDataException("fromDate khong duoc sau toDate");
        }
        return new DateRange(start, end);
    }

    private Map<String, Long> toStatusMap(List<ReportRepository.StatusCountProjection> projections) {
        return projections.stream().collect(Collectors.toMap(
                ReportRepository.StatusCountProjection::getStatus,
                item -> longValue(item.getTotal())
        ));
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private long longValue(Long value) {
        return value == null ? 0 : value;
    }

    private record DateRange(LocalDate fromDate, LocalDate toDate) {
    }
}
