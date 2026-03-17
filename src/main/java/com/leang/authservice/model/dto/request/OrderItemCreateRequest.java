package com.leang.authservice.model.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemCreateRequest {
    private UUID orderId;
    private UUID productId;
    private Integer quantity;
    private BigDecimal price;
}

