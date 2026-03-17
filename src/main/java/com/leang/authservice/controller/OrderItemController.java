package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.OrderItemCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.OrderItem;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.repository.ProductRepository;
import com.leang.authservice.service.OrderItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/order-items")
@RequiredArgsConstructor
@Tag(name = "OrderItem")
@SecurityRequirement(name = "bearerAuth")
public class OrderItemController extends BaseResponse {

    private final OrderItemService orderItemService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Operation(summary = "Create order item")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderItem>> create(@RequestBody OrderItemCreateRequest dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(dto.getQuantity())
                .price(dto.getPrice())
                .build();

        return responseEntity(true, "Order item created successfully.", HttpStatus.CREATED, orderItemService.create(orderItem));
    }

    @Operation(summary = "Get order item by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderItem>> getById(@PathVariable UUID id) {
        return responseEntity(true, "Order item retrieved successfully.", HttpStatus.OK, orderItemService.getById(id));
    }

    @Operation(summary = "Get all order items")
    @GetMapping
    public ResponseEntity<ApiResponseWithPagination<OrderItem>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ApiResponseWithPagination<OrderItem> response = orderItemService.getAll(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Update order item")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderItem>> update(@PathVariable UUID id, @RequestBody OrderItem orderItem) {
        return responseEntity(true, "Order item updated successfully.", HttpStatus.OK, orderItemService.update(id, orderItem));
    }

    @Operation(summary = "Delete order item")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        orderItemService.delete(id);
        return responseEntity(true, "Order item deleted successfully.", HttpStatus.OK);
    }
}

