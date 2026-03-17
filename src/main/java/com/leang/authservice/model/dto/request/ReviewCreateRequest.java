package com.leang.authservice.model.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class ReviewCreateRequest {
    private UUID userId;
    private UUID productId;
    private Integer rating;
    private String comment;
}

