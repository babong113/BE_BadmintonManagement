package com.bteam.platform.core.order.service;

import com.bteam.platform.adapter.persistence.jpa.entity.UserEntity;
import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.customer.repository.CustomerRepository;
import com.bteam.platform.core.order.dto.CreateOrderRequest;
import com.bteam.platform.core.order.dto.OrderItemRequest;
import com.bteam.platform.core.order.dto.OrderItemResponse;
import com.bteam.platform.core.order.dto.OrderResponse;
import com.bteam.platform.core.order.dto.UpdateOrderItemRequest;
import com.bteam.platform.core.order.model.Order;
import com.bteam.platform.core.order.model.OrderItem;
import com.bteam.platform.core.order.model.OrderStatus;
import com.bteam.platform.core.order.repository.OrderRepository;
import com.bteam.platform.core.product.model.Product;
import com.bteam.platform.core.product.model.ProductStatus;
import com.bteam.platform.core.product.repository.ProductRepository;
import com.bteam.platform.core.venue.model.Venue;
import com.bteam.platform.core.venue.model.VenueStatus;
import com.bteam.platform.core.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter ORDER_CODE_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final VenueRepository venueRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Authentication authentication) {
        UserEntity actor = findUserByEmail(authentication.getName());
        Venue venue = venueRepository.findByIdAndStatus(request.venueId(), VenueStatus.ACTIVE)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay co so dang hoat dong"));

        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .venue(venue)
                .status(OrderStatus.PENDING)
                .build();
        applyCustomerIdentity(order, request, actor, authentication);

        Set<Long> productIds = new HashSet<>();
        if (request.items() != null) {
            for (OrderItemRequest itemRequest : request.items()) {
                if (!productIds.add(itemRequest.productId())) {
                    throw new InvalidDataException("Moi san pham chi duoc xuat hien mot lan trong order");
                }
                order.addItem(createOrderItem(itemRequest));
            }
        }
        recalculateTotal(order);
        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id, Authentication authentication) {
        Order order = findOrder(id);
        ensureCanAccess(order, authentication);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Authentication authentication) {
        UserEntity actor = findUserByEmail(authentication.getName());
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(actor.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForManagement(OrderStatus status, Long venueId) {
        return orderRepository.findForManagement(status, venueId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse addItem(
            Long orderId,
            OrderItemRequest request,
            Authentication authentication
    ) {
        Order order = findOrderForUpdate(orderId);
        ensureCanModify(order, authentication);
        if (order.getItems().stream().anyMatch(item -> item.getProduct().getId().equals(request.productId()))) {
            throw new InvalidDataException("San pham da ton tai trong order");
        }

        order.addItem(createOrderItem(request));
        recalculateTotal(order);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateItemQuantity(
            Long orderId,
            Long itemId,
            UpdateOrderItemRequest request,
            Authentication authentication
    ) {
        Order order = findOrderForUpdate(orderId);
        ensureCanModify(order, authentication);
        OrderItem item = findItem(order, itemId);
        validateProductAvailable(item.getProduct(), request.quantity());

        item.setQuantity(request.quantity());
        item.setSubtotal(money(item.getUnitPrice().multiply(BigDecimal.valueOf(request.quantity()))));
        recalculateTotal(order);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse removeItem(Long orderId, Long itemId, Authentication authentication) {
        Order order = findOrderForUpdate(orderId);
        ensureCanModify(order, authentication);
        OrderItem item = findItem(order, itemId);
        order.getItems().remove(item);
        recalculateTotal(order);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse confirm(Long orderId) {
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidDataException("Chi order PENDING moi co the xac nhan");
        }
        if (order.getItems().isEmpty()) {
            throw new InvalidDataException("Khong the xac nhan order khong co san pham");
        }
        for (OrderItem item : order.getItems()) {
            Product currentProduct = findProduct(item.getProduct().getId());
            validateProductAvailable(currentProduct, item.getQuantity());
        }
        recalculateTotal(order);
        order.setStatus(OrderStatus.CONFIRMED);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse complete(Long orderId) {
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidDataException("Chi order CONFIRMED moi co the hoan tat");
        }

        Map<Long, Integer> quantitiesByProduct = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            quantitiesByProduct.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        }

        List<Long> sortedProductIds = quantitiesByProduct.keySet().stream().sorted().toList();
        for (Long productId : sortedProductIds) {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new InvalidDataException("Khong tim thay san pham"));
            int requestedQuantity = quantitiesByProduct.get(productId);
            validateProductAvailable(product, requestedQuantity);
            product.setQuantity(product.getQuantity() - requestedQuantity);
            if (product.getQuantity() == 0) {
                product.setStatus(ProductStatus.OUT_OF_STOCK);
            }
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.COMPLETED);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancel(Long orderId, Authentication authentication) {
        Order order = findOrderForUpdate(orderId);
        ensureCanAccess(order, authentication);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new InvalidDataException("Khong the huy order da hoan tat");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidDataException("Order da duoc huy truoc do");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    private OrderItem createOrderItem(OrderItemRequest request) {
        Product product = findProduct(request.productId());
        validateProductAvailable(product, request.quantity());
        BigDecimal unitPrice = money(product.getSalePrice());
        return OrderItem.builder()
                .product(product)
                .quantity(request.quantity())
                .unitPrice(unitPrice)
                .subtotal(money(unitPrice.multiply(BigDecimal.valueOf(request.quantity()))))
                .build();
    }

    private void applyCustomerIdentity(
            Order order,
            CreateOrderRequest request,
            UserEntity actor,
            Authentication authentication
    ) {
        boolean staffFlow = hasAuthority(authentication, "order:update")
                || hasAuthority(authentication, "order:read");
        if (!staffFlow) {
            if (request.customerId() != null
                    || trimToNull(request.guestName()) != null
                    || trimToNull(request.guestPhone()) != null) {
                throw new InvalidDataException("Khach hang chi duoc tao order cho chinh minh");
            }
            order.setCustomer(actor);
            return;
        }

        if (request.customerId() != null) {
            if (trimToNull(request.guestName()) != null || trimToNull(request.guestPhone()) != null) {
                throw new InvalidDataException("Order chi duoc gan customer hoac khach vang lai");
            }
            order.setCustomer(customerRepository.findCustomerById(request.customerId())
                    .orElseThrow(() -> new InvalidDataException("Khong tim thay khach hang")));
            return;
        }

        String guestName = trimToNull(request.guestName());
        String guestPhone = trimToNull(request.guestPhone());
        if (guestName == null || guestPhone == null) {
            throw new InvalidDataException("Can customerId hoac day du thong tin khach vang lai");
        }
        order.setGuestName(guestName);
        order.setGuestPhone(guestPhone);
    }

    private void ensureCanModify(Order order, Authentication authentication) {
        ensureCanAccess(order, authentication);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidDataException("Chi duoc thay doi san pham khi order PENDING");
        }
    }

    private void ensureCanAccess(Order order, Authentication authentication) {
        if (hasAuthority(authentication, "order:read") || hasAuthority(authentication, "order:update")) {
            return;
        }
        if (order.getCustomer() != null
                && order.getCustomer().getEmail().equalsIgnoreCase(authentication.getName())) {
            return;
        }
        throw new InvalidDataException("Ban khong co quyen truy cap order nay");
    }

    private void validateProductAvailable(Product product, int requestedQuantity) {
        if (!product.isForSale() || product.getSalePrice() == null) {
            throw new InvalidDataException("San pham khong ho tro ban");
        }
        if (product.getStatus() != ProductStatus.AVAILABLE) {
            throw new InvalidDataException("San pham khong o trang thai AVAILABLE");
        }
        if (requestedQuantity > product.getQuantity()) {
            throw new InvalidDataException("So luong mua vuot qua ton kho");
        }
    }

    private void recalculateTotal(Order order) {
        BigDecimal total = order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(money(total));
    }

    private Order findOrder(Long id) {
        return orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay order"));
    }

    private Order findOrderForUpdate(Long id) {
        return orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay order"));
    }

    private OrderItem findItem(Order order, Long itemId) {
        return order.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new InvalidDataException("Khong tim thay order item trong order"));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay san pham"));
    }

    private UserEntity findUserByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidDataException("Nguoi dung khong ton tai"));
    }

    private String generateOrderCode() {
        String prefix = "ORD" + LocalDate.now().format(ORDER_CODE_DATE_FORMAT);
        String code;
        do {
            code = prefix + "-" + randomDigits(6);
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }

    private String randomDigits(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getVenue().getId(),
                order.getVenue().getName(),
                order.getCustomer() == null ? null : order.getCustomer().getId(),
                order.getCustomer() == null ? null : order.getCustomer().getFullName(),
                order.getGuestName(),
                order.getGuestPhone(),
                money(order.getTotalAmount()),
                order.getStatus(),
                order.getCreatedAt(),
                order.getItems().stream().map(this::toItemResponse).toList()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(), item.getProduct().getId(), item.getProduct().getName(),
                item.getQuantity(), money(item.getUnitPrice()), money(item.getSubtotal())
        );
    }
}
