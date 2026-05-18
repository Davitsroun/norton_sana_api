package com.leang.authservice.model.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminRevenueChartPointResponse(
        Instant periodStart,
        BigDecimal revenue
) {
}
