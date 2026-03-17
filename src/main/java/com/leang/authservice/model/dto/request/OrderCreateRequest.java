package com.leang.authservice.model.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderCreateRequest {
    private UUID userId;
    private BigDecimal totalPrice;
    private String status;
}

