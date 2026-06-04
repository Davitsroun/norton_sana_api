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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        long ordersLast30 = orderRepository.countRevenueOrdersBetween(thirtyDaysAgo, now);
        long ordersPrev30 = orderRepository.countRevenueOrdersBetween(sixtyDaysAgo, thirtyDaysAgo);
        double ordersDelta = percentChange(ordersPrev30, ordersLast30);

        long usersLast30 = userProfileRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(thirtyDaysAgo, now);
        long usersPrev30 = userProfileRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(sixtyDaysAgo, thirtyDaysAgo);
        double usersDelta = percentChange(usersPrev30, usersLast30);

        BigDecimal currentRevenue = nvl(orderRepository.sumRevenueBetween(thirtyDaysAgo, now));
        BigDecimal previousRevenue = nvl(orderRepository.sumRevenueBetween(sixtyDaysAgo, thirtyDaysAgo));
        double revenueGrowth = percentChange(previousRevenue, currentRevenue);

        long usersTotal = userProfileRepository.count();
        BigDecimal revenue = orderRepository.sumPaidRevenue();
        if (revenue == null) {
            revenue = BigDecimal.ZERO;
        }

        AdminDashboardSummaryResponse body = new AdminDashboardSummaryResponse(
                revenue,
                orderRepository.countRevenueOrders(),
                usersTotal,
                revenueGrowth,
                ordersDelta,
                usersDelta
        );
        return responseEntity(true, "Dashboard summary loaded", HttpStatus.OK, body);
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<ApiResponse<List<AdminRevenueChartPointResponse>>> revenueChart(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        ZonedDateTime nowMonthUtc = ZonedDateTime.now(ZoneOffset.UTC)
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);

        Instant start = (from != null
                ? from.atStartOfDay(ZoneOffset.UTC)
                : nowMonthUtc.minusMonths(11)).toInstant();

        Instant endExclusive = (to != null
                ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC)
                : nowMonthUtc.plusMonths(1)).toInstant();

        Map<Instant, BigDecimal> dbRevenueByMonth = new LinkedHashMap<>();
        for (Object[] row : orderRepository.revenueByMonth(start, endExclusive)) {
            Instant period = normalizeToUtcMonthStart(toInstant(row[0]));
            BigDecimal rev = row[1] instanceof BigDecimal b
                    ? b
                    : BigDecimal.valueOf(((Number) row[1]).doubleValue());
            dbRevenueByMonth.put(period, rev.setScale(2, RoundingMode.HALF_UP));
        }

        ZonedDateTime cursor = start.atZone(ZoneOffset.UTC)
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime endMonth = endExclusive.minusMillis(1)
                .atZone(ZoneOffset.UTC)
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);

        List<AdminRevenueChartPointResponse> points = new java.util.ArrayList<>();
        while (!cursor.isAfter(endMonth)) {
            Instant periodStart = normalizeToUtcMonthStart(cursor.toInstant());
            BigDecimal revenue = dbRevenueByMonth.getOrDefault(periodStart, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            points.add(new AdminRevenueChartPointResponse(periodStart, revenue));
            cursor = cursor.plusMonths(1);
        }

        return responseEntity(true, "Revenue chart loaded", HttpStatus.OK, points);
    }

    private static double percentChange(long previous, long current) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return roundPercent(((double) (current - previous) / previous) * 100.0);
    }

    private static double percentChange(BigDecimal previous, BigDecimal current) {
        BigDecimal prev = previous == null ? BigDecimal.ZERO : previous;
        BigDecimal curr = current == null ? BigDecimal.ZERO : current;
        if (prev.compareTo(BigDecimal.ZERO) == 0) {
            return curr.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return roundPercent(curr.subtract(prev)
                .divide(prev, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue());
    }

    private static double roundPercent(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toInstant(ZoneOffset.UTC);
        }
        if (value instanceof java.time.LocalDateTime ldt) {
            return ldt.toInstant(ZoneOffset.UTC);
        }
        throw new IllegalArgumentException("Unsupported periodStart type: " + (value == null ? "null" : value.getClass().getName()));
    }

    private static Instant normalizeToUtcMonthStart(Instant instant) {
        return instant.atZone(ZoneOffset.UTC)
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
    }
}
