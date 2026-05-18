package com.leang.authservice.controller;

import com.leang.authservice.model.dto.response.AdminDashboardSummaryResponse;
import com.leang.authservice.model.dto.response.AdminRevenueChartPointResponse;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.repository.UserProfileRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin dashboard")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController extends BaseResponse {

    private final OrderRepository orderRepository;
    private final UserProfileRepository userProfileRepository;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryResponse>> summary() {
        Instant now = Instant.now();
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        Instant sixtyDaysAgo = now.minus(60, ChronoUnit.DAYS);

        long ordersLast30 = orderRepository.countOrdersBetween(thirtyDaysAgo, now);
        long ordersPrev30 = orderRepository.countOrdersBetween(sixtyDaysAgo, thirtyDaysAgo);
        double ordersDelta = percentChange(ordersPrev30, ordersLast30);

        long usersTotal = userProfileRepository.count();
        BigDecimal revenue = orderRepository.sumPaidRevenue();
        if (revenue == null) {
            revenue = BigDecimal.ZERO;
        }

        AdminDashboardSummaryResponse body = new AdminDashboardSummaryResponse(
                revenue,
                orderRepository.count(),
                usersTotal,
                ordersDelta,
                ordersDelta,
                0.0
        );
        return responseEntity(true, "Dashboard summary retrieved successfully.", HttpStatus.OK, body);
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<ApiResponse<List<AdminRevenueChartPointResponse>>> revenueChart(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Instant end = to != null ? to : Instant.now();
        Instant start = from != null ? from : end.minus(365, ChronoUnit.DAYS);
        List<AdminRevenueChartPointResponse> points = orderRepository.revenueByMonth(start, end).stream()
                .map(row -> {
                    Instant period = ((Timestamp) row[0]).toInstant();
                    BigDecimal rev = row[1] instanceof BigDecimal b
                            ? b
                            : BigDecimal.valueOf(((Number) row[1]).doubleValue());
                    return new AdminRevenueChartPointResponse(period, rev.setScale(2, RoundingMode.HALF_UP));
                })
                .toList();
        return responseEntity(true, "Revenue chart retrieved successfully.", HttpStatus.OK, points);
    }

    private static double percentChange(long previous, long current) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((double) (current - previous) / previous) * 100.0;
    }
}
