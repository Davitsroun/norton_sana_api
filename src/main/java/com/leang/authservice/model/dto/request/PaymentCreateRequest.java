package com.leang.authservice.model.dto.request;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class PaymentCreateRequest {
    private UUID orderId;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;
    private Instant paidAt;
}

