package com.leang.authservice.model.dto.response;

public record AdminStatisticsResponse(
        long totalUsers,
        long totalProducts,
        long totalOrders
) {
}
