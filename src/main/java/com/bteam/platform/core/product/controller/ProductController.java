package com.bteam.platform.core.product.controller;

import com.bteam.platform.core.common.response.ApiResponse;
import com.bteam.platform.core.product.dto.CreateProductRequest;
import com.bteam.platform.core.product.dto.ProductResponse;
import com.bteam.platform.core.product.dto.UpdateProductRequest;
import com.bteam.platform.core.product.dto.UpdateStockRequest;
import com.bteam.platform.core.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAuthority('equipment:read')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAvailableProducts(
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                keyword == null || keyword.isBlank()
                        ? "Lay danh sach san pham thanh cong"
                        : "Tim kiem san pham thanh cong",
                productService.getAvailableProducts(keyword)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('equipment:read')")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay chi tiet san pham thanh cong",
                productService.getProduct(id)
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Tao san pham thanh cong",
                productService.createProduct(request)
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cap nhat san pham thanh cong",
                productService.updateProduct(id, request)
        ));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('STAFF', 'OWNER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStockRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cap nhat ton kho thanh cong",
                productService.updateStock(id, request)
        ));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<ProductResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Ngung kinh doanh san pham thanh cong",
                productService.deactivate(id)
        ));
    }
}
