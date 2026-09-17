package com.bteam.platform.core.report.controller;

import com.bteam.platform.core.common.response.ApiResponse;
import com.bteam.platform.core.report.dto.BookingReportResponse;
import com.bteam.platform.core.report.dto.CancellationRateResponse;
import com.bteam.platform.core.report.dto.CourtRevenueResponse;
import com.bteam.platform.core.report.dto.CustomerReportResponse;
import com.bteam.platform.core.report.dto.DashboardResponse;
import com.bteam.platform.core.report.dto.OrderReportResponse;
import com.bteam.platform.core.report.dto.PeakHourResponse;
import com.bteam.platform.core.report.dto.PeriodRevenueResponse;
import com.bteam.platform.core.report.dto.PopularCourtResponse;
import com.bteam.platform.core.report.dto.ProductPerformanceResponse;
import com.bteam.platform.core.report.dto.RevenueSummaryResponse;
import com.bteam.platform.core.report.dto.ReviewPerformanceResponse;
import com.bteam.platform.core.report.dto.VenueRevenueResponse;
import com.bteam.platform.core.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('report:read')")
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay dashboard thanh cong", reportService.getDashboard(fromDate, toDate));
    }

    @GetMapping("/revenue/summary")
    public ResponseEntity<ApiResponse<RevenueSummaryResponse>> getRevenueSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay tong doanh thu thanh cong", reportService.getRevenueSummary(fromDate, toDate));
    }

    @GetMapping("/revenue/daily")
    public ResponseEntity<ApiResponse<List<PeriodRevenueResponse>>> getDailyRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay doanh thu theo ngay thanh cong", reportService.getDailyRevenue(fromDate, toDate));
    }

    @GetMapping("/revenue/monthly")
    public ResponseEntity<ApiResponse<List<PeriodRevenueResponse>>> getMonthlyRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay doanh thu theo thang thanh cong", reportService.getMonthlyRevenue(fromDate, toDate));
    }

    @GetMapping("/revenue/venues")
    public ResponseEntity<ApiResponse<List<VenueRevenueResponse>>> getRevenueByVenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay doanh thu theo co so thanh cong", reportService.getRevenueByVenue(fromDate, toDate));
    }

    @GetMapping("/revenue/courts")
    public ResponseEntity<ApiResponse<List<CourtRevenueResponse>>> getRevenueByCourt(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay doanh thu theo san thanh cong", reportService.getRevenueByCourt(fromDate, toDate));
    }

    @GetMapping("/bookings/summary")
    public ResponseEntity<ApiResponse<BookingReportResponse>> getBookingReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay thong ke booking thanh cong", reportService.getBookingReport(fromDate, toDate));
    }

    @GetMapping("/bookings/cancellation-rate")
    public ResponseEntity<ApiResponse<CancellationRateResponse>> getCancellationRate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay ty le huy booking thanh cong", reportService.getCancellationRate(fromDate, toDate));
    }

    @GetMapping("/bookings/peak-hours")
    public ResponseEntity<ApiResponse<List<PeakHourResponse>>> getPeakHours(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay khung gio cao diem thanh cong", reportService.getPeakHours(fromDate, toDate));
    }

    @GetMapping("/bookings/popular-courts")
    public ResponseEntity<ApiResponse<List<PopularCourtResponse>>> getPopularCourts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay thong ke san pho bien thanh cong", reportService.getPopularCourts(fromDate, toDate));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<CustomerReportResponse>> getCustomerReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay thong ke khach hang thanh cong", reportService.getCustomerReport(fromDate, toDate));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductPerformanceResponse>>> getProductReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay thong ke san pham thanh cong", reportService.getProductReport(fromDate, toDate));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<OrderReportResponse>> getOrderReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay thong ke order thanh cong", reportService.getOrderReport(fromDate, toDate));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<List<ReviewPerformanceResponse>>> getReviewReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ok("Lay thong ke danh gia thanh cong", reportService.getReviewReport(fromDate, toDate));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, message, data));
    }
}
