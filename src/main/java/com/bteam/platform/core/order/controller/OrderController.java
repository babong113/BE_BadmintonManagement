package com.bteam.platform.core.order.controller;

import com.bteam.platform.core.common.response.ApiResponse;
import com.bteam.platform.core.order.dto.CreateOrderRequest;
import com.bteam.platform.core.order.dto.OrderItemRequest;
import com.bteam.platform.core.order.dto.OrderResponse;
import com.bteam.platform.core.order.dto.UpdateOrderItemRequest;
import com.bteam.platform.core.order.model.OrderStatus;
import com.bteam.platform.core.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('order:create')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Tao order thanh cong",
                orderService.createOrder(request, authentication)
        ));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('order:create')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay lich su order thanh cong",
                orderService.getMyOrders(authentication)
        ));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order:read')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersForManagement(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long venueId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay danh sach order thanh cong",
                orderService.getOrdersForManagement(status, venueId)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('order:create', 'order:read')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lay chi tiet order thanh cong",
                orderService.getOrder(id, authentication)
        ));
    }

    @PostMapping("/{orderId}/items")
    @PreAuthorize("hasAnyAuthority('order:create', 'order:update')")
    public ResponseEntity<ApiResponse<OrderResponse>> addItem(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderItemRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Them san pham vao order thanh cong",
                orderService.addItem(orderId, request, authentication)
        ));
    }

    @PatchMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasAnyAuthority('order:create', 'order:update')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateItemQuantity(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateOrderItemRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cap nhat so luong order item thanh cong",
                orderService.updateItemQuantity(orderId, itemId, request, authentication)
        ));
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasAnyAuthority('order:create', 'order:update')")
    public ResponseEntity<ApiResponse<OrderResponse>> removeItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Xoa san pham khoi order thanh cong",
                orderService.removeItem(orderId, itemId, authentication)
        ));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('order:update')")
    public ResponseEntity<ApiResponse<OrderResponse>> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Xac nhan order thanh cong",
                orderService.confirm(id)
        ));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('order:update')")
    public ResponseEntity<ApiResponse<OrderResponse>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Hoan tat order thanh cong",
                orderService.complete(id)
        ));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('order:create', 'order:update')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Huy order thanh cong",
                orderService.cancel(id, authentication)
        ));
    }
}
