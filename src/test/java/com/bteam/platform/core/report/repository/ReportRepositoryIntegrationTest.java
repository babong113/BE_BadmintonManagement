package com.bteam.platform.core.report.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReportRepositoryIntegrationTest {
    @Autowired
    private ReportRepository reportRepository;

    @Test
    void allReportQueriesExecuteAgainstPostgres() {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 12, 31);

        assertThat(reportRepository.findRevenueTotals(fromDate, toDate).getTotalRevenue()).isNotNull();
        assertThat(reportRepository.findDailyRevenue(fromDate, toDate)).isNotNull();
        assertThat(reportRepository.findMonthlyRevenue(fromDate, toDate)).isNotNull();
        assertThat(reportRepository.findRevenueByVenue(fromDate, toDate)).isNotNull();
        assertThat(reportRepository.findRevenueByCourt(fromDate, toDate)).isNotNull();
        assertThat(reportRepository.findBookingStatusCounts(fromDate, toDate)).isNotNull();
        assertThat(reportRepository.findDailyBookingCounts(fromDate, toDate)).isNotNull();
        assertThat(reportRepository.findMonthlyBookingCounts(fromDate, toDate)).isNotNull();
        assertThat(reportRepository.findCancellationCounts(fromDate, toDate).getTotalBookings()).isNotNull();
        assertThat(reportRepository.findPeakHours(fromDate, toDate)).isNotNull();
        assertThat(reportRepository.findPopularCourts(fromDate, toDate)).isNotNull();
        assertThat(reportRepository.findCustomerActivity(fromDate, toDate)).isNotNull();
        assertThat(reportRepository.findProductPerformance(fromDate, toDate)).isNotNull();
        assertThat(reportRepository.findOrderTotals(fromDate, toDate).getTotalOrders()).isNotNull();
        assertThat(reportRepository.findOrderStatusCounts(fromDate, toDate)).isNotNull();
        assertThat(reportRepository.findReviewPerformance(fromDate, toDate)).isNotNull();
        assertThat(reportRepository.findDashboardCounts(fromDate, toDate).getActiveVenues()).isNotNull();
    }
}
