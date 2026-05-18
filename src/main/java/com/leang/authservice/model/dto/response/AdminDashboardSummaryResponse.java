package com.leang.authservice.model.dto.response;

import java.math.BigDecimal;

public record AdminDashboardSummaryResponse(
        BigDecimal totalRevenue,
        long totalOrders,
        long totalUsers,
        double growthRatePercent,
        double ordersDeltaPercent,
        double usersDeltaPercent
) {
}
