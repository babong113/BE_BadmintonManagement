package com.bteam.platform.core.order.service;

import com.bteam.platform.adapter.persistence.jpa.entity.UserEntity;
import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.customer.repository.CustomerRepository;
import com.bteam.platform.core.order.dto.CreateOrderRequest;
import com.bteam.platform.core.order.dto.OrderItemRequest;
import com.bteam.platform.core.order.dto.OrderResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private CustomerRepository customerRepository;

    private OrderService orderService;
    private UserEntity customer;
    private Venue venue;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository, productRepository, venueRepository, customerRepository
        );
        customer = new UserEntity();
        customer.setId(10L);
        customer.setEmail("customer@example.com");
        customer.setFullName("Customer One");
        venue = Venue.builder()
                .id(1L)
                .name("Central Venue")
                .status(VenueStatus.ACTIVE)
                .build();
    }

    @Test
    void customerCreatesOrderForSelfAndSystemCalculatesAmounts() {
        Product product = saleProduct(5, "120000.00");
        mockCreateDependencies(product);

        OrderResponse response = orderService.createOrder(
                new CreateOrderRequest(
                        1L, null, null, null,
                        List.of(new OrderItemRequest(5L, 2))
                ),
                customerAuthentication()
        );

        assertThat(response.orderCode()).startsWith("ORD");
        assertThat(response.customerId()).isEqualTo(10L);
        assertThat(response.guestName()).isNull();
        assertThat(response.items().getFirst().unitPrice()).isEqualByComparingTo("120000.00");
        assertThat(response.items().getFirst().subtotal()).isEqualByComparingTo("240000.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("240000.00");
        assertThat(product.getQuantity()).isEqualTo(5);
    }

    @Test
    void customerCannotCreateOrderForAnotherIdentity() {
        when(customerRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(venueRepository.findByIdAndStatus(1L, VenueStatus.ACTIVE)).thenReturn(Optional.of(venue));

        CreateOrderRequest request = new CreateOrderRequest(1L, 99L, null, null, null);

        assertThatThrownBy(() -> orderService.createOrder(request, customerAuthentication()))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Khach hang chi duoc tao order cho chinh minh");
    }

    @Test
    void staffCanCreateCounterOrderForGuest() {
        UserEntity staff = new UserEntity();
        staff.setEmail("staff@example.com");
        when(customerRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));
        when(venueRepository.findByIdAndStatus(1L, VenueStatus.ACTIVE)).thenReturn(Optional.of(venue));
        when(orderRepository.existsByOrderCode(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(
                new CreateOrderRequest(1L, null, "Guest One", "0901234567", null),
                staffAuthentication()
        );

        assertThat(response.customerId()).isNull();
        assertThat(response.guestName()).isEqualTo("Guest One");
        assertThat(response.guestPhone()).isEqualTo("0901234567");
    }

    @Test
    void completingOrderDecrementsStockAndMarksOutOfStock() {
        Product product = saleProduct(2, "120000.00");
        Order order = confirmedOrder(product, 2);
        when(orderRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response = orderService.complete(20L);

        assertThat(response.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(product.getQuantity()).isZero();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void completingOrderRechecksStock() {
        Product product = saleProduct(1, "120000.00");
        Order order = confirmedOrder(product, 2);
        when(orderRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.complete(20L))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("So luong mua vuot qua ton kho");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    private void mockCreateDependencies(Product product) {
        when(customerRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(venueRepository.findByIdAndStatus(1L, VenueStatus.ACTIVE)).thenReturn(Optional.of(venue));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(orderRepository.existsByOrderCode(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(20L);
            order.getItems().forEach(item -> item.setId(30L));
            return order;
        });
    }

    private Order confirmedOrder(Product product, int quantity) {
        Order order = Order.builder()
                .id(20L)
                .orderCode("ORD-001")
                .venue(venue)
                .customer(customer)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("240000.00"))
                .items(new ArrayList<>())
                .build();
        order.addItem(OrderItem.builder()
                .id(30L)
                .product(product)
                .quantity(quantity)
                .unitPrice(new BigDecimal("120000.00"))
                .subtotal(new BigDecimal("240000.00"))
                .build());
        return order;
    }

    private Product saleProduct(int quantity, String price) {
        return Product.builder()
                .id(5L)
                .name("Ong cau long")
                .quantity(quantity)
                .salePrice(new BigDecimal(price))
                .forSale(true)
                .status(ProductStatus.AVAILABLE)
                .build();
    }

    private Authentication customerAuthentication() {
        return authentication("customer@example.com", "order:create");
    }

    private Authentication staffAuthentication() {
        return authentication("staff@example.com", "order:update");
    }

    private Authentication authentication(String email, String authority) {
        return new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
