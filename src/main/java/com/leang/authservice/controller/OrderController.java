package com.leang.authservice.controller;

import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order")
@SecurityRequirement(name = "bearerAuth")
public class OrderController extends BaseResponse {

    private final OrderService orderService;

    @Operation(summary = "Get order by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getById(@PathVariable UUID id) {
        return responseEntity(true, "Order retrieved successfully.", HttpStatus.OK, orderService.getById(id));
    }

    @Operation(summary = "Get all orders")
    @GetMapping
    public ResponseEntity<ApiResponseWithPagination<Order>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ApiResponseWithPagination<Order> response = orderService.getAll(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Delete order")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        orderService.delete(id);
        return responseEntity(true, "Order deleted successfully.", HttpStatus.OK);
    }
}

