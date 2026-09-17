package com.bteam.platform.core.report.service;

import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.report.dto.CancellationRateResponse;
import com.bteam.platform.core.report.dto.CustomerReportResponse;
import com.bteam.platform.core.report.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock
    private ReportRepository reportRepository;

    @Test
    void cancellationRateIsReturnedAsPercentage() {
        ReportService service = new ReportService(reportRepository);
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        ReportRepository.CancellationProjection projection = cancellationProjection(8L, 2L);
        when(reportRepository.findCancellationCounts(from, to)).thenReturn(projection);

        CancellationRateResponse response = service.getCancellationRate(from, to);

        assertThat(response.cancellationRate()).isEqualByComparingTo("25.00");
    }

    @Test
    void customerReportCalculatesAverageBookingFrequency() {
        ReportService service = new ReportService(reportRepository);
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        when(reportRepository.findCustomerActivity(from, to)).thenReturn(List.of(
                customerProjection(1L, 3L),
                customerProjection(2L, 2L)
        ));

        CustomerReportResponse response = service.getCustomerReport(from, to);

        assertThat(response.totalCustomers()).isEqualTo(2);
        assertThat(response.averageBookingsPerCustomer()).isEqualByComparingTo("2.50");
    }

    @Test
    void reportRejectsReversedDateRange() {
        ReportService service = new ReportService(reportRepository);

        assertThatThrownBy(() -> service.getDailyRevenue(
                LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 9, 1)
        ))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("fromDate khong duoc sau toDate");
    }

    private ReportRepository.CancellationProjection cancellationProjection(long total, long cancelled) {
        return new ReportRepository.CancellationProjection() {
            public Long getTotalBookings() {
                return total;
            }

            public Long getCancelledBookings() {
                return cancelled;
            }
        };
    }

    private ReportRepository.CustomerActivityProjection customerProjection(Long id, Long bookings) {
        return new ReportRepository.CustomerActivityProjection() {
            public Long getCustomerId() {
                return id;
            }

            public String getCustomerName() {
                return "Customer " + id;
            }

            public String getEmail() {
                return "customer" + id + "@example.com";
            }

            public Long getBookingCount() {
                return bookings;
            }

            public Long getCompletedBookingCount() {
                return bookings;
            }

            public LocalDate getLastBookingDate() {
                return LocalDate.of(2026, 9, 15);
            }
        };
    }
}
