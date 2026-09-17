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
import com.bteam.platform.core.payment.dto.AddAllocationsRequest;
import com.bteam.platform.core.payment.dto.AllocationRequest;
import com.bteam.platform.core.payment.dto.CreatePaymentRequest;
import com.bteam.platform.core.payment.dto.PaymentAllocationResponse;
import com.bteam.platform.core.payment.dto.PaymentResponse;
import com.bteam.platform.core.payment.dto.PaymentSummaryResponse;
import com.bteam.platform.core.payment.dto.UpdatePaymentStatusRequest;
import com.bteam.platform.core.payment.model.Payment;
import com.bteam.platform.core.payment.model.CashMovement;
import com.bteam.platform.core.payment.model.CashMovementSourceType;
import com.bteam.platform.core.payment.model.CashMovementType;
import com.bteam.platform.core.payment.model.CashierShift;
import com.bteam.platform.core.payment.model.CashierShiftStatus;
import com.bteam.platform.core.payment.model.PaymentAllocation;
import com.bteam.platform.core.payment.model.PaymentMethod;
import com.bteam.platform.core.payment.model.PaymentStatus;
import com.bteam.platform.core.payment.repository.PaymentAllocationRepository;
import com.bteam.platform.core.payment.repository.PaymentRepository;
import com.bteam.platform.core.payment.repository.CashMovementRepository;
import com.bteam.platform.core.payment.repository.CashierShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final Set<PaymentStatus> RESERVED_STATUSES =
            EnumSet.of(PaymentStatus.PENDING, PaymentStatus.PAID);

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;
    private final BookingUserRepository userRepository;
    private final CashierShiftRepository cashierShiftRepository;
    private final CashMovementRepository cashMovementRepository;

    @Transactional(readOnly = true)
    public PaymentSummaryResponse getBookingSummary(Long bookingId, Authentication authentication) {
        Booking booking = findBooking(bookingId);
        ensureCanAccessBooking(booking, authentication);

        BigDecimal paid = sumByStatuses(bookingId, Set.of(PaymentStatus.PAID));
        BigDecimal pending = sumByStatuses(bookingId, Set.of(PaymentStatus.PENDING));
        BigDecimal refunded = sumByStatuses(bookingId, Set.of(PaymentStatus.REFUNDED));
        BigDecimal remaining = booking.getTotalAmount().subtract(paid).max(BigDecimal.ZERO);

        return new PaymentSummaryResponse(
                booking.getId(),
                booking.getBookingCode(),
                null,
                null,
                money(booking.getTotalAmount()),
                money(paid),
                money(pending),
                money(refunded),
                money(remaining)
        );
    }

    @Transactional(readOnly = true)
    public PaymentSummaryResponse getOrderSummary(Long orderId, Authentication authentication) {
        Order order = findOrder(orderId);
        ensureCanAccessOrder(order, authentication);

        BigDecimal paid = sumOrderByStatuses(orderId, Set.of(PaymentStatus.PAID));
        BigDecimal pending = sumOrderByStatuses(orderId, Set.of(PaymentStatus.PENDING));
        BigDecimal refunded = sumOrderByStatuses(orderId, Set.of(PaymentStatus.REFUNDED));
        BigDecimal remaining = order.getTotalAmount().subtract(paid).max(BigDecimal.ZERO);

        return new PaymentSummaryResponse(
                null,
                null,
                order.getId(),
                order.getOrderCode(),
                money(order.getTotalAmount()),
                money(paid),
                money(pending),
                money(refunded),
                money(remaining)
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getBookingPayments(Long bookingId, Authentication authentication) {
        Booking booking = findBooking(bookingId);
        ensureCanAccessBooking(booking, authentication);
        return paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getOrderPayments(Long orderId, Authentication authentication) {
        Order order = findOrder(orderId);
        ensureCanAccessOrder(order, authentication);
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PaymentResponse createPayment(
            Long bookingId,
            CreatePaymentRequest request,
            Authentication authentication
    ) {
        Booking booking = bookingRepository.findWithDetailsByIdForUpdate(bookingId)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay booking"));
        ensureCanAccessBooking(booking, authentication);
        ensureBookingCanReceivePayment(booking);

        boolean canWrite = hasAuthority(authentication, "payment:write");
        if (request.paymentMethod() == PaymentMethod.CASH && !canWrite) {
            throw new InvalidDataException("Chi nhan vien moi duoc ghi nhan thanh toan tien mat");
        }
        if (hasAllocations(request.allocations()) && !canWrite) {
            throw new InvalidDataException("Chi nhan vien moi duoc phan bo thanh toan");
        }

        BigDecimal amount = requireMoney(request.amount(), "So tien thanh toan");
        validateBookingDebt(booking, amount);

        PaymentStatus initialStatus = request.paymentMethod() == PaymentMethod.CASH
                ? PaymentStatus.PAID
                : PaymentStatus.PENDING;
        String transactionCode = trimToNull(request.transactionCode());

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(amount)
                .paymentMethod(request.paymentMethod())
                .paymentStatus(initialStatus)
                .transactionCode(transactionCode)
                .paidAt(initialStatus == PaymentStatus.PAID ? ZonedDateTime.now() : null)
                .build();

        if (hasAllocations(request.allocations())) {
            addValidatedAllocations(payment, booking, request.allocations());
        }

        UserEntity receiver = null;
        CashierShift cashierShift = null;
        if (initialStatus == PaymentStatus.PAID) {
            receiver = findUserByEmail(authentication.getName());
            cashierShift = findOpenShift(receiver, booking.getVenue().getId());
            payment.setReceivedBy(receiver);
            payment.setCashierShift(cashierShift);
        }

        Payment savedPayment = paymentRepository.save(payment);
        if (initialStatus == PaymentStatus.PAID) {
            recordCashMovement(savedPayment, cashierShift, receiver, CashMovementType.CASH_IN);
        }
        return toResponse(savedPayment);
    }

    @Transactional
    public PaymentResponse createOrderPayment(
            Long orderId,
            CreatePaymentRequest request,
            Authentication authentication
    ) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay order"));
        ensureCanAccessOrder(order, authentication);
        ensureOrderCanReceivePayment(order);
        if (hasAllocations(request.allocations())) {
            throw new InvalidDataException("Payment cua order khong ho tro phan bo booking detail");
        }

        boolean canWrite = hasAuthority(authentication, "payment:write");
        if (request.paymentMethod() == PaymentMethod.CASH && !canWrite) {
            throw new InvalidDataException("Chi nhan vien moi duoc ghi nhan thanh toan tien mat");
        }

        BigDecimal amount = requireMoney(request.amount(), "So tien thanh toan");
        validateOrderDebt(order, amount);
        PaymentStatus initialStatus = request.paymentMethod() == PaymentMethod.CASH
                ? PaymentStatus.PAID
                : PaymentStatus.PENDING;
        UserEntity receiver = null;
        CashierShift cashierShift = null;
        if (initialStatus == PaymentStatus.PAID) {
            receiver = findUserByEmail(authentication.getName());
            cashierShift = findOpenShift(receiver, order.getVenue().getId());
        }

        Payment payment = Payment.builder()
                .order(order)
                .amount(amount)
                .paymentMethod(request.paymentMethod())
                .paymentStatus(initialStatus)
                .transactionCode(trimToNull(request.transactionCode()))
                .paidAt(initialStatus == PaymentStatus.PAID ? ZonedDateTime.now() : null)
                .receivedBy(receiver)
                .cashierShift(cashierShift)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        if (initialStatus == PaymentStatus.PAID) {
            recordCashMovement(savedPayment, cashierShift, receiver, CashMovementType.CASH_IN);
        }
        return toResponse(savedPayment);
    }

    @Transactional
    public PaymentResponse updateStatus(
            Long paymentId,
            UpdatePaymentStatusRequest request,
            Authentication authentication
    ) {
        Payment payment = findPaymentForUpdate(paymentId);
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new InvalidDataException("Chi payment PENDING moi duoc cap nhat thanh PAID hoac FAILED");
        }
        if (request.status() != PaymentStatus.PAID && request.status() != PaymentStatus.FAILED) {
            throw new InvalidDataException("Trang thai moi chi co the la PAID hoac FAILED");
        }

        String transactionCode = trimToNull(request.transactionCode());
        if (request.status() == PaymentStatus.PAID
                && payment.getPaymentMethod() != PaymentMethod.CASH
                && transactionCode == null
                && payment.getTransactionCode() == null) {
            throw new InvalidDataException("Can ma giao dich khi xac nhan thanh toan thanh cong");
        }

        if (transactionCode != null) {
            payment.setTransactionCode(transactionCode);
        }
        payment.setPaymentStatus(request.status());
        payment.setPaidAt(request.status() == PaymentStatus.PAID ? ZonedDateTime.now() : null);
        if (request.status() == PaymentStatus.PAID) {
            payment.setReceivedBy(findUserByEmail(authentication.getName()));
        }
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse addAllocations(Long paymentId, AddAllocationsRequest request) {
        Payment payment = findPaymentForUpdate(paymentId);
        if (payment.getPaymentStatus() == PaymentStatus.FAILED
                || payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new InvalidDataException("Khong the phan bo payment FAILED hoac REFUNDED");
        }

        Booking booking = bookingRepository.findWithDetailsByIdForUpdate(payment.getBooking().getId())
                .orElseThrow(() -> new InvalidDataException("Khong tim thay booking"));
        payment.setBooking(booking);
        addValidatedAllocations(payment, booking, request.allocations());
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse refund(Long paymentId, Authentication authentication) {
        Payment payment = findPaymentForUpdate(paymentId);
        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
            throw new InvalidDataException("Chi payment PAID moi co the hoan tien");
        }

        UserEntity actor = findUserByEmail(authentication.getName());
        CashierShift refundShift = null;
        if (payment.getPaymentMethod() == PaymentMethod.CASH) {
            refundShift = findOpenShift(actor, targetVenueId(payment));
        }

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        Payment savedPayment = paymentRepository.save(payment);
        if (payment.getPaymentMethod() == PaymentMethod.CASH) {
            recordCashMovement(savedPayment, refundShift, actor, CashMovementType.CASH_OUT);
        }
        return toResponse(savedPayment);
    }

    private void validateBookingDebt(Booking booking, BigDecimal requestedAmount) {
        BigDecimal reserved = sumByStatuses(booking.getId(), RESERVED_STATUSES);
        BigDecimal availableDebt = booking.getTotalAmount().subtract(reserved);
        if (requestedAmount.compareTo(availableDebt) > 0) {
            throw new InvalidDataException("So tien thanh toan vuot qua cong no con lai");
        }
    }

    private void validateOrderDebt(Order order, BigDecimal requestedAmount) {
        BigDecimal reserved = sumOrderByStatuses(order.getId(), RESERVED_STATUSES);
        BigDecimal availableDebt = order.getTotalAmount().subtract(reserved);
        if (requestedAmount.compareTo(availableDebt) > 0) {
            throw new InvalidDataException("So tien thanh toan vuot qua cong no con lai cua order");
        }
    }

    private void addValidatedAllocations(
            Payment payment,
            Booking booking,
            List<AllocationRequest> requests
    ) {
        Map<Long, BookingDetail> detailsById = booking.getDetails()
                .stream()
                .collect(Collectors.toMap(BookingDetail::getId, detail -> detail));
        Set<Long> requestedDetailIds = new HashSet<>();
        Map<Long, BigDecimal> requestedByDetail = new HashMap<>();
        BigDecimal requestedTotal = BigDecimal.ZERO;

        for (AllocationRequest request : requests) {
            if (!requestedDetailIds.add(request.bookingDetailId())) {
                throw new InvalidDataException("Moi booking detail chi duoc xuat hien mot lan trong request");
            }
            BookingDetail detail = detailsById.get(request.bookingDetailId());
            if (detail == null) {
                throw new InvalidDataException("Booking detail khong thuoc booking cua payment");
            }
            BigDecimal amount = requireMoney(request.amount(), "So tien phan bo");
            requestedByDetail.put(detail.getId(), amount);
            requestedTotal = requestedTotal.add(amount);
        }

        BigDecimal currentPaymentAllocation = payment.getAllocations()
                .stream()
                .map(PaymentAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (currentPaymentAllocation.add(requestedTotal).compareTo(payment.getAmount()) > 0) {
            throw new InvalidDataException("Tong so tien phan bo vuot qua so tien payment");
        }

        for (Map.Entry<Long, BigDecimal> entry : requestedByDetail.entrySet()) {
            BookingDetail detail = detailsById.get(entry.getKey());
            BigDecimal activeAllocated = allocationRepository.sumActiveAmountByBookingDetail(
                    detail.getId(), RESERVED_STATUSES
            );
            if (activeAllocated.add(entry.getValue()).compareTo(detail.getSubtotal()) > 0) {
                throw new InvalidDataException(
                        "So tien phan bo vuot qua cong no cua booking detail " + detail.getId()
                );
            }

            payment.addAllocation(PaymentAllocation.builder()
                    .bookingDetail(detail)
                    .amount(entry.getValue())
                    .build());
        }
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findWithDetailsById(bookingId)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay booking"));
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay order"));
    }

    private Payment findPaymentForUpdate(Long paymentId) {
        return paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay payment"));
    }

    private void ensureBookingCanReceivePayment(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new InvalidDataException("Khong the tao payment cho booking da huy hoac het han");
        }
        if (booking.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDataException("Booking khong co cong no can thanh toan");
        }
    }

    private void ensureOrderCanReceivePayment(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidDataException("Khong the tao payment cho order da huy");
        }
        if (order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDataException("Order khong co cong no can thanh toan");
        }
    }

    private void ensureCanAccessBooking(Booking booking, Authentication authentication) {
        if (hasAuthority(authentication, "payment:write")) {
            return;
        }
        if (booking.getCustomer() != null
                && booking.getCustomer().getEmail().equalsIgnoreCase(authentication.getName())) {
            return;
        }
        throw new InvalidDataException("Ban khong co quyen truy cap thanh toan cua booking nay");
    }

    private void ensureCanAccessOrder(Order order, Authentication authentication) {
        if (hasAuthority(authentication, "payment:write")) {
            return;
        }
        if (order.getCustomer() != null
                && order.getCustomer().getEmail().equalsIgnoreCase(authentication.getName())) {
            return;
        }
        throw new InvalidDataException("Ban khong co quyen truy cap thanh toan cua order nay");
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private boolean hasAllocations(List<AllocationRequest> allocations) {
        return allocations != null && !allocations.isEmpty();
    }

    private BigDecimal sumByStatuses(Long bookingId, Set<PaymentStatus> statuses) {
        BigDecimal result = paymentRepository.sumAmountByBookingAndStatuses(bookingId, statuses);
        return result == null ? BigDecimal.ZERO : result;
    }

    private BigDecimal sumOrderByStatuses(Long orderId, Set<PaymentStatus> statuses) {
        BigDecimal result = paymentRepository.sumAmountByOrderAndStatuses(orderId, statuses);
        return result == null ? BigDecimal.ZERO : result;
    }

    private UserEntity findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidDataException("Nguoi dung khong ton tai"));
    }

    private CashierShift findOpenShift(UserEntity staff, Long venueId) {
        if (staff == null) {
            throw new InvalidDataException("Khong xac dinh duoc nhan vien thu tien");
        }
        return cashierShiftRepository.findByStaffIdAndVenueIdAndStatus(
                        staff.getId(), venueId, CashierShiftStatus.OPEN
                )
                .orElseThrow(() -> new InvalidDataException(
                        "Nhan vien khong co ca thu ngan OPEN tai co so cua giao dich"
                ));
    }

    private Long targetVenueId(Payment payment) {
        if (payment.getBooking() != null) {
            return payment.getBooking().getVenue().getId();
        }
        return payment.getOrder().getVenue().getId();
    }

    private void recordCashMovement(
            Payment payment,
            CashierShift shift,
            UserEntity actor,
            CashMovementType movementType
    ) {
        CashMovementSourceType sourceType = movementType == CashMovementType.CASH_OUT
                ? CashMovementSourceType.REFUND
                : payment.getBooking() != null
                ? CashMovementSourceType.BOOKING_PAYMENT
                : CashMovementSourceType.ORDER_PAYMENT;
        cashMovementRepository.save(CashMovement.builder()
                .cashierShift(shift)
                .movementType(movementType)
                .sourceType(sourceType)
                .amount(payment.getAmount())
                .referenceId(payment.getId())
                .description(movementType == CashMovementType.CASH_IN
                        ? "Thu tien payment " + payment.getId()
                        : "Hoan tien payment " + payment.getId())
                .createdBy(actor)
                .build());
    }

    private BigDecimal requireMoney(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDataException(fieldName + " phai lon hon 0");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new InvalidDataException(fieldName + " chi duoc co toi da 2 chu so thap phan");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PaymentResponse toResponse(Payment payment) {
        List<PaymentAllocationResponse> allocations = payment.getAllocations()
                .stream()
                .map(allocation -> new PaymentAllocationResponse(
                        allocation.getId(),
                        allocation.getBookingDetail().getId(),
                        money(allocation.getAmount())
                ))
                .toList();
        BigDecimal allocatedAmount = payment.getAllocations()
                .stream()
                .map(PaymentAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PaymentResponse(
                payment.getId(),
                payment.getBooking() == null ? null : payment.getBooking().getId(),
                payment.getBooking() == null ? null : payment.getBooking().getBookingCode(),
                payment.getOrder() == null ? null : payment.getOrder().getId(),
                payment.getOrder() == null ? null : payment.getOrder().getOrderCode(),
                money(payment.getAmount()),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getTransactionCode(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                payment.getReceivedBy() == null ? null : payment.getReceivedBy().getId(),
                payment.getReceivedBy() == null ? null : payment.getReceivedBy().getFullName(),
                payment.getCashierShift() == null ? null : payment.getCashierShift().getId(),
                money(allocatedAmount),
                money(payment.getAmount().subtract(allocatedAmount)),
                allocations
        );
    }
}
