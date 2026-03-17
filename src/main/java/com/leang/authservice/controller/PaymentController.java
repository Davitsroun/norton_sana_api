package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.PaymentCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.Payment;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController extends BaseResponse {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    @Operation(summary = "Create payment")
    @PostMapping
    public ResponseEntity<ApiResponse<Payment>> create(@RequestBody PaymentCreateRequest dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(dto.getPaymentMethod())
                .paymentStatus(dto.getPaymentStatus())
                .transactionId(dto.getTransactionId())
                .paidAt(dto.getPaidAt())
                .build();

        return responseEntity(true, "Payment created successfully.", HttpStatus.CREATED, paymentService.create(payment));
    }

    @Operation(summary = "Get payment by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Payment>> getById(@PathVariable UUID id) {
        return responseEntity(true, "Payment retrieved successfully.", HttpStatus.OK, paymentService.getById(id));
    }

    @Operation(summary = "Get all payments")
    @GetMapping
    public ResponseEntity<ApiResponseWithPagination<Payment>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ApiResponseWithPagination<Payment> response = paymentService.getAll(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Update payment")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Payment>> update(@PathVariable UUID id, @RequestBody Payment payment) {
        return responseEntity(true, "Payment updated successfully.", HttpStatus.OK, paymentService.update(id, payment));
    }

    @Operation(summary = "Delete payment")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        paymentService.delete(id);
        return responseEntity(true, "Payment deleted successfully.", HttpStatus.OK);
    }
}

