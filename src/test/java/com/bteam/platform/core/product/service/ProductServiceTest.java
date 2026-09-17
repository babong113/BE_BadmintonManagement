package com.bteam.platform.core.product.service;

import com.bteam.platform.adapter.persistence.jpa.entity.UserEntity;
import com.bteam.platform.core.booking.model.Booking;
import com.bteam.platform.core.booking.model.BookingDetail;
import com.bteam.platform.core.booking.model.BookingDetailStatus;
import com.bteam.platform.core.booking.model.BookingStatus;
import com.bteam.platform.core.booking.repository.BookingDetailRepository;
import com.bteam.platform.core.booking.repository.BookingRepository;
import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.product.dto.BookingEquipmentResponse;
import com.bteam.platform.core.product.dto.CreateProductRequest;
import com.bteam.platform.core.product.dto.RentProductRequest;
import com.bteam.platform.core.product.dto.UpdateStockRequest;
import com.bteam.platform.core.product.model.BookingEquipment;
import com.bteam.platform.core.product.model.Product;
import com.bteam.platform.core.product.model.ProductStatus;
import com.bteam.platform.core.product.repository.BookingEquipmentRepository;
import com.bteam.platform.core.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BookingEquipmentRepository bookingEquipmentRepository;
    @Mock
    private BookingDetailRepository bookingDetailRepository;
    @Mock
    private BookingRepository bookingRepository;

    private ProductService productService;
    private BookingDetail detail;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository, bookingEquipmentRepository, bookingDetailRepository, bookingRepository
        );

        UserEntity customer = new UserEntity();
        customer.setId(10L);
        customer.setEmail("customer@example.com");
        Booking booking = Booking.builder()
                .id(1L)
                .bookingCode("BKG-001")
                .customer(customer)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("200000.00"))
                .build();
        detail = BookingDetail.builder()
                .id(11L)
                .booking(booking)
                .status(BookingDetailStatus.ACTIVE)
                .subtotal(new BigDecimal("200000.00"))
                .build();
        booking.setDetails(List.of(detail));
    }

    @Test
    void createProductRequiresSaleOrRentalClassification() {
        CreateProductRequest request = new CreateProductRequest(
                "Cau long", 10, null, null, false, false
        );

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("San pham phai duoc phan loai ban, thue hoac ca hai");
    }

    @Test
    void createProductWithZeroQuantityIsOutOfStock() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = productService.createProduct(new CreateProductRequest(
                "Vot cau long", 0, new BigDecimal("500000.00"), null, true, false
        ));

        assertThat(response.status()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void restockingOutOfStockProductMakesItAvailable() {
        Product product = rentableProduct(0);
        product.setStatus(ProductStatus.OUT_OF_STOCK);
        when(productRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        var response = productService.updateStock(5L, new UpdateStockRequest(8));

        assertThat(response.quantity()).isEqualTo(8);
        assertThat(response.status()).isEqualTo(ProductStatus.AVAILABLE);
    }

    @Test
    void rentRejectsQuantityGreaterThanStock() {
        when(bookingDetailRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(detail));
        when(bookingRepository.findWithDetailsByIdForUpdate(1L)).thenReturn(Optional.of(detail.getBooking()));
        when(productRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(rentableProduct(2)));

        assertThatThrownBy(() -> productService.rentForBookingDetail(
                11L, new RentProductRequest(5L, 3), customerAuthentication()
        ))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("So luong thue vuot qua ton kho");
    }

    @Test
    void rentCalculatesSubtotalUpdatesBookingAndMarksProductOutOfStock() {
        Product product = rentableProduct(2);
        when(bookingDetailRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(detail));
        when(bookingRepository.findWithDetailsByIdForUpdate(1L)).thenReturn(Optional.of(detail.getBooking()));
        when(productRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(bookingEquipmentRepository.save(any(BookingEquipment.class))).thenAnswer(invocation -> {
            BookingEquipment rental = invocation.getArgument(0);
            rental.setId(50L);
            return rental;
        });
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingEquipmentResponse response = productService.rentForBookingDetail(
                11L, new RentProductRequest(5L, 2), customerAuthentication()
        );

        assertThat(response.unitPrice()).isEqualByComparingTo("30000.00");
        assertThat(response.subtotal()).isEqualByComparingTo("60000.00");
        assertThat(product.getQuantity()).isZero();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
        assertThat(detail.getBooking().getTotalAmount()).isEqualByComparingTo("260000.00");
    }

    private Product rentableProduct(int quantity) {
        return Product.builder()
                .id(5L)
                .name("Vot cau long")
                .quantity(quantity)
                .rentalPrice(new BigDecimal("30000.00"))
                .forRent(true)
                .status(quantity == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.AVAILABLE)
                .build();
    }

    private Authentication customerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "customer@example.com",
                null,
                List.of(new SimpleGrantedAuthority("booking:create"))
        );
    }
}
