package com.bteam.platform.core.payment.service;

import com.bteam.platform.adapter.persistence.jpa.entity.UserEntity;
import com.bteam.platform.core.booking.model.Booking;
import com.bteam.platform.core.booking.model.BookingDetail;
import com.bteam.platform.core.booking.model.BookingStatus;
import com.bteam.platform.core.booking.repository.BookingRepository;
import com.bteam.platform.core.booking.repository.BookingUserRepository;
import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.order.model.Order;
import com.bteam.platform.core.order.model.OrderStatus;
import com.bteam.platform.core.order.repository.OrderRepository;
import com.bteam.platform.core.payment.dto.AllocationRequest;
import com.bteam.platform.core.payment.dto.CreatePaymentRequest;
import com.bteam.platform.core.payment.dto.PaymentResponse;
import com.bteam.platform.core.payment.dto.UpdatePaymentStatusRequest;
import com.bteam.platform.core.payment.model.Payment;
import com.bteam.platform.core.payment.model.CashMovement;
import com.bteam.platform.core.payment.model.CashMovementType;
import com.bteam.platform.core.payment.model.CashierShift;
import com.bteam.platform.core.payment.model.CashierShiftStatus;
import com.bteam.platform.core.payment.model.PaymentMethod;
import com.bteam.platform.core.payment.model.PaymentStatus;
import com.bteam.platform.core.payment.repository.PaymentAllocationRepository;
import com.bteam.platform.core.payment.repository.PaymentRepository;
import com.bteam.platform.core.payment.repository.CashMovementRepository;
import com.bteam.platform.core.payment.repository.CashierShiftRepository;
import com.bteam.platform.core.venue.model.Venue;
import com.bteam.platform.core.venue.model.VenueStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentAllocationRepository allocationRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private BookingUserRepository userRepository;
    @Mock
    private CashierShiftRepository cashierShiftRepository;
    @Mock
    private CashMovementRepository cashMovementRepository;

    private PaymentService paymentService;
    private Booking booking;
    private UserEntity staff;
    private Venue venue;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                allocationRepository,
                bookingRepository,
                orderRepository,
                userRepository,
                cashierShiftRepository,
                cashMovementRepository
        );

        UserEntity customer = new UserEntity();
        customer.setId(10L);
        customer.setEmail("customer@example.com");

        staff = new UserEntity();
        staff.setId(20L);
        staff.setEmail("staff@example.com");
        staff.setFullName("Staff");

        venue = Venue.builder()
                .id(5L)
                .name("Co so 1")
                .address("Dia chi")
                .status(VenueStatus.ACTIVE)
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(101L)
                .subtotal(new BigDecimal("300000.00"))
                .build();
        booking = Booking.builder()
                .id(1L)
                .bookingCode("BKG-001")
                .venue(venue)
                .customer(customer)
                .totalAmount(new BigDecimal("300000.00"))
                .status(BookingStatus.CONFIRMED)
                .details(List.of(detail))
                .build();
        detail.setBooking(booking);
    }

    @Test
    void customerCannotRecordCashPayment() {
        when(bookingRepository.findWithDetailsByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("100000.00"), PaymentMethod.CASH, null, null);

        assertThatThrownBy(() -> paymentService.createPayment(1L, request, customerAuthentication()))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Chi nhan vien moi duoc ghi nhan thanh toan tien mat");
    }

    @Test
    void paymentCannotExceedRemainingDebt() {
        when(bookingRepository.findWithDetailsByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.sumAmountByBookingAndStatuses(any(), anySet()))
                .thenReturn(new BigDecimal("250000.00"));

        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("100000.00"), PaymentMethod.CASH, null, null);

        assertThatThrownBy(() -> paymentService.createPayment(1L, request, staffAuthentication()))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("So tien thanh toan vuot qua cong no con lai");
    }

    @Test
    void allocationCannotExceedDetailDebt() {
        when(bookingRepository.findWithDetailsByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.sumAmountByBookingAndStatuses(any(), anySet())).thenReturn(BigDecimal.ZERO);
        when(allocationRepository.sumActiveAmountByBookingDetail(any(), anySet()))
                .thenReturn(new BigDecimal("250000.00"));

        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("100000.00"),
                PaymentMethod.CASH,
                null,
                List.of(new AllocationRequest(101L, new BigDecimal("100000.00")))
        );

        assertThatThrownBy(() -> paymentService.createPayment(1L, request, staffAuthentication()))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("So tien phan bo vuot qua cong no cua booking detail 101");
    }

    @Test
    void successfulGatewayCallbackStoresTransactionAndPaidTime() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(userRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));

        PaymentResponse response = paymentService.updateStatus(
                20L,
                new UpdatePaymentStatusRequest(PaymentStatus.PAID, "VNPAY-123"),
                staffAuthentication()
        );

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.transactionCode()).isEqualTo("VNPAY-123");
        assertThat(response.paidAt()).isNotNull();
        assertThat(response.receivedById()).isEqualTo(20L);
    }

    @Test
    void paidPaymentCanBeRefunded() {
        Payment payment = pendingPayment();
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(ZonedDateTime.now());
        when(paymentRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(userRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));

        PaymentResponse response = paymentService.refund(20L, staffAuthentication());

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void cashPaymentStoresReceiverShiftAndCashInMovement() {
        CashierShift shift = openShift();
        when(bookingRepository.findWithDetailsByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.sumAmountByBookingAndStatuses(any(), anySet())).thenReturn(BigDecimal.ZERO);
        when(userRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));
        when(cashierShiftRepository.findByStaffIdAndVenueIdAndStatus(20L, 5L, CashierShiftStatus.OPEN))
                .thenReturn(Optional.of(shift));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(30L);
            return payment;
        });

        PaymentResponse response = paymentService.createPayment(
                1L,
                new CreatePaymentRequest(new BigDecimal("100000.00"), PaymentMethod.CASH, null, null),
                staffAuthentication()
        );

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.receivedById()).isEqualTo(20L);
        assertThat(response.cashierShiftId()).isEqualTo(50L);
        verify(cashMovementRepository).save(any(CashMovement.class));
    }

    @Test
    void cashPaymentRequiresOpenShiftAtTargetVenue() {
        when(bookingRepository.findWithDetailsByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.sumAmountByBookingAndStatuses(any(), anySet())).thenReturn(BigDecimal.ZERO);
        when(userRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));
        when(cashierShiftRepository.findByStaffIdAndVenueIdAndStatus(20L, 5L, CashierShiftStatus.OPEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment(
                1L,
                new CreatePaymentRequest(new BigDecimal("100000.00"), PaymentMethod.CASH, null, null),
                staffAuthentication()
        ))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Nhan vien khong co ca thu ngan OPEN tai co so cua giao dich");
    }

    @Test
    void orderSupportsPartialPaymentAndOrderTargetResponse() {
        Order order = Order.builder()
                .id(7L)
                .orderCode("ORD-007")
                .venue(venue)
                .customer(booking.getCustomer())
                .totalAmount(new BigDecimal("200000.00"))
                .status(OrderStatus.CONFIRMED)
                .build();
        when(orderRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(order));
        when(paymentRepository.sumAmountByOrderAndStatuses(any(), anySet())).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(40L);
            return payment;
        });

        PaymentResponse response = paymentService.createOrderPayment(
                7L,
                new CreatePaymentRequest(
                        new BigDecimal("50000.00"), PaymentMethod.BANK_TRANSFER, null, null
                ),
                customerAuthentication()
        );

        assertThat(response.bookingId()).isNull();
        assertThat(response.orderId()).isEqualTo(7L);
        assertThat(response.orderCode()).isEqualTo("ORD-007");
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void orderSummaryCalculatesPaidPendingAndRemainingAmounts() {
        Order order = Order.builder()
                .id(7L)
                .orderCode("ORD-007")
                .venue(venue)
                .customer(booking.getCustomer())
                .totalAmount(new BigDecimal("200000.00"))
                .status(OrderStatus.CONFIRMED)
                .build();
        when(orderRepository.findWithItemsById(7L)).thenReturn(Optional.of(order));
        when(paymentRepository.sumAmountByOrderAndStatuses(eq(7L), eq(java.util.Set.of(PaymentStatus.PAID))))
                .thenReturn(new BigDecimal("50000.00"));
        when(paymentRepository.sumAmountByOrderAndStatuses(eq(7L), eq(java.util.Set.of(PaymentStatus.PENDING))))
                .thenReturn(new BigDecimal("25000.00"));
        when(paymentRepository.sumAmountByOrderAndStatuses(eq(7L), eq(java.util.Set.of(PaymentStatus.REFUNDED))))
                .thenReturn(new BigDecimal("10000.00"));

        var response = paymentService.getOrderSummary(7L, customerAuthentication());

        assertThat(response.orderId()).isEqualTo(7L);
        assertThat(response.paidAmount()).isEqualByComparingTo("50000.00");
        assertThat(response.pendingAmount()).isEqualByComparingTo("25000.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("150000.00");
    }

    @Test
    void cashRefundCreatesCashOutMovementInCurrentOpenShift() {
        Payment payment = Payment.builder()
                .id(20L)
                .booking(booking)
                .amount(new BigDecimal("100000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.PAID)
                .paidAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .build();
        CashierShift shift = openShift();
        when(paymentRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));
        when(cashierShiftRepository.findByStaffIdAndVenueIdAndStatus(20L, 5L, CashierShiftStatus.OPEN))
                .thenReturn(Optional.of(shift));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(cashMovementRepository.save(any(CashMovement.class))).thenAnswer(invocation -> {
            CashMovement movement = invocation.getArgument(0);
            assertThat(movement.getMovementType()).isEqualTo(CashMovementType.CASH_OUT);
            return movement;
        });

        paymentService.refund(20L, staffAuthentication());

        verify(cashMovementRepository).save(any(CashMovement.class));
    }

    private Payment pendingPayment() {
        return Payment.builder()
                .id(20L)
                .booking(booking)
                .amount(new BigDecimal("100000.00"))
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(ZonedDateTime.now())
                .build();
    }

    private Authentication customerAuthentication() {
        return authentication("customer@example.com", "payment:read");
    }

    private CashierShift openShift() {
        return CashierShift.builder()
                .id(50L)
                .staff(staff)
                .venue(venue)
                .openingCash(new BigDecimal("500000.00"))
                .status(CashierShiftStatus.OPEN)
                .build();
    }

    private Authentication staffAuthentication() {
        return authentication("staff@example.com", "payment:write");
    }

    private Authentication authentication(String email, String authority) {
        return new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
