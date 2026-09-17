package com.bteam.platform.core.product.service;

import com.bteam.platform.core.booking.model.Booking;
import com.bteam.platform.core.booking.model.BookingDetail;
import com.bteam.platform.core.booking.model.BookingDetailStatus;
import com.bteam.platform.core.booking.model.BookingStatus;
import com.bteam.platform.core.booking.repository.BookingDetailRepository;
import com.bteam.platform.core.booking.repository.BookingRepository;
import com.bteam.platform.core.common.exception.InvalidDataException;
import com.bteam.platform.core.product.dto.BookingEquipmentResponse;
import com.bteam.platform.core.product.dto.CreateProductRequest;
import com.bteam.platform.core.product.dto.ProductResponse;
import com.bteam.platform.core.product.dto.RentProductRequest;
import com.bteam.platform.core.product.dto.UpdateProductRequest;
import com.bteam.platform.core.product.dto.UpdateStockRequest;
import com.bteam.platform.core.product.model.BookingEquipment;
import com.bteam.platform.core.product.model.Product;
import com.bteam.platform.core.product.model.ProductStatus;
import com.bteam.platform.core.product.repository.BookingEquipmentRepository;
import com.bteam.platform.core.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final BookingEquipmentRepository bookingEquipmentRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAvailableProducts(String keyword) {
        String normalizedKeyword = trimToNull(keyword);
        List<Product> products = normalizedKeyword == null
                ? productRepository.findByStatusAndQuantityGreaterThanOrderByNameAsc(ProductStatus.AVAILABLE, 0)
                : productRepository.findByStatusAndQuantityGreaterThanAndNameContainingIgnoreCaseOrderByNameAsc(
                        ProductStatus.AVAILABLE, 0, normalizedKeyword
                );
        return products.stream().map(this::toProductResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return toProductResponse(findProduct(id));
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        validateClassification(
                request.isForSale(), request.isForRent(), request.salePrice(), request.rentalPrice()
        );
        Product product = Product.builder()
                .name(request.name().trim())
                .quantity(request.quantity())
                .salePrice(optionalMoney(request.salePrice(), "Gia ban"))
                .rentalPrice(optionalMoney(request.rentalPrice(), "Gia thue"))
                .forSale(request.isForSale())
                .forRent(request.isForRent())
                .status(statusForQuantity(request.quantity()))
                .build();
        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = findProductForUpdate(id);
        if (request.status() == ProductStatus.OUT_OF_STOCK) {
            throw new InvalidDataException("OUT_OF_STOCK duoc he thong cap nhat theo ton kho");
        }
        validateClassification(
                request.isForSale(), request.isForRent(), request.salePrice(), request.rentalPrice()
        );

        product.setName(request.name().trim());
        product.setSalePrice(optionalMoney(request.salePrice(), "Gia ban"));
        product.setRentalPrice(optionalMoney(request.rentalPrice(), "Gia thue"));
        product.setForSale(request.isForSale());
        product.setForRent(request.isForRent());
        product.setStatus(request.status() == ProductStatus.INACTIVE
                ? ProductStatus.INACTIVE
                : statusForQuantity(product.getQuantity()));
        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateStock(Long id, UpdateStockRequest request) {
        Product product = findProductForUpdate(id);
        product.setQuantity(request.quantity());
        if (product.getStatus() != ProductStatus.INACTIVE) {
            product.setStatus(statusForQuantity(request.quantity()));
        }
        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse deactivate(Long id) {
        Product product = findProductForUpdate(id);
        product.setStatus(ProductStatus.INACTIVE);
        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public BookingEquipmentResponse rentForBookingDetail(
            Long bookingDetailId,
            RentProductRequest request,
            Authentication authentication
    ) {
        BookingDetail detail = bookingDetailRepository.findByIdForUpdate(bookingDetailId)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay booking detail"));
        Booking lockedBooking = bookingRepository.findWithDetailsByIdForUpdate(detail.getBooking().getId())
                .orElseThrow(() -> new InvalidDataException("Khong tim thay booking"));
        detail.setBooking(lockedBooking);
        ensureCanRentForDetail(detail, authentication);

        Product product = findProductForUpdate(request.productId());
        validateRentable(product, request.quantity());

        BigDecimal unitPrice = money(product.getRentalPrice());
        BigDecimal subtotal = money(unitPrice.multiply(BigDecimal.valueOf(request.quantity())));
        BookingEquipment rental = BookingEquipment.builder()
                .bookingDetail(detail)
                .product(product)
                .quantity(request.quantity())
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .build();

        product.setQuantity(product.getQuantity() - request.quantity());
        if (product.getQuantity() == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }
        productRepository.save(product);

        BookingEquipment savedRental = bookingEquipmentRepository.save(rental);
        detail.getEquipmentRentals().add(savedRental);
        lockedBooking.setTotalAmount(money(lockedBooking.getTotalAmount().add(subtotal)));
        bookingRepository.save(lockedBooking);

        return toBookingEquipmentResponse(savedRental);
    }

    @Transactional(readOnly = true)
    public List<BookingEquipmentResponse> getBookingDetailRentals(
            Long bookingDetailId,
            Authentication authentication
    ) {
        BookingDetail detail = bookingDetailRepository.findWithBookingById(bookingDetailId)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay booking detail"));
        ensureCanAccessDetail(detail, authentication);
        return bookingEquipmentRepository.findByBookingDetailIdOrderByIdAsc(bookingDetailId)
                .stream()
                .map(this::toBookingEquipmentResponse)
                .toList();
    }

    private void validateRentable(Product product, int requestedQuantity) {
        if (!product.isForRent() || product.getRentalPrice() == null) {
            throw new InvalidDataException("San pham khong ho tro cho thue");
        }
        if (product.getStatus() != ProductStatus.AVAILABLE) {
            throw new InvalidDataException("San pham khong o trang thai AVAILABLE");
        }
        if (requestedQuantity > product.getQuantity()) {
            throw new InvalidDataException("So luong thue vuot qua ton kho");
        }
    }

    private void ensureCanRentForDetail(BookingDetail detail, Authentication authentication) {
        Booking booking = detail.getBooking();
        ensureCanAccessDetail(detail, authentication);
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.COMPLETED
                || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new InvalidDataException("Khong the them dung cu vao booking da huy, hoan tat hoac het han");
        }
        if (detail.getStatus() != BookingDetailStatus.ACTIVE) {
            throw new InvalidDataException("Chi booking detail ACTIVE moi duoc thue dung cu");
        }
    }

    private void ensureCanAccessDetail(BookingDetail detail, Authentication authentication) {
        if (hasAuthority(authentication, "booking:update")) {
            return;
        }
        if (detail.getBooking().getCustomer() != null
                && detail.getBooking().getCustomer().getEmail().equalsIgnoreCase(authentication.getName())) {
            return;
        }
        throw new InvalidDataException("Ban khong co quyen truy cap booking detail nay");
    }

    private void validateClassification(
            boolean forSale,
            boolean forRent,
            BigDecimal salePrice,
            BigDecimal rentalPrice
    ) {
        if (!forSale && !forRent) {
            throw new InvalidDataException("San pham phai duoc phan loai ban, thue hoac ca hai");
        }
        if (forSale && salePrice == null) {
            throw new InvalidDataException("San pham de ban phai co gia ban");
        }
        if (forRent && rentalPrice == null) {
            throw new InvalidDataException("San pham cho thue phai co gia thue");
        }
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay san pham"));
    }

    private Product findProductForUpdate(Long id) {
        return productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new InvalidDataException("Khong tim thay san pham"));
    }

    private ProductStatus statusForQuantity(int quantity) {
        return quantity == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.AVAILABLE;
    }

    private BigDecimal optionalMoney(BigDecimal value, String fieldName) {
        return value == null ? null : requireMoney(value, fieldName);
    }

    private BigDecimal requireMoney(BigDecimal value, String fieldName) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidDataException(fieldName + " khong duoc am");
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

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(), product.getName(), product.getQuantity(),
                product.getSalePrice(), product.getRentalPrice(),
                product.isForSale(), product.isForRent(), product.getStatus(), product.getCreatedAt()
        );
    }

    private BookingEquipmentResponse toBookingEquipmentResponse(BookingEquipment rental) {
        return new BookingEquipmentResponse(
                rental.getId(), rental.getBookingDetail().getId(), rental.getProduct().getId(),
                rental.getProduct().getName(), rental.getQuantity(),
                money(rental.getUnitPrice()), money(rental.getSubtotal())
        );
    }
}
