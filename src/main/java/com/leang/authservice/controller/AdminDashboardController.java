package com.leang.authservice.controller;

import com.leang.authservice.model.dto.response.AdminDashboardSummaryResponse;
import com.leang.authservice.model.dto.response.AdminRevenueChartPointResponse;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.repository.OrderItemRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    private final OrderItemRepository orderItemRepository;
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
        BigDecimal totalRevenue = scale2(nvl(orderRepository.sumPaidRevenue()));
        BigDecimal totalCost = scale2(nvl(orderItemRepository.sumPaidCost()));
        BigDecimal totalProfit = scale2(totalRevenue.subtract(totalCost));

        AdminDashboardSummaryResponse body = new AdminDashboardSummaryResponse(
                totalRevenue,
                totalCost,
                totalProfit,
                profitMarginPercent(totalRevenue, totalProfit),
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "groupBy", defaultValue = "month") String groupBy
    ) {
        return responseEntity(true, "Revenue chart loaded", HttpStatus.OK, buildProfitChart(from, to, groupBy));
    }

    @GetMapping("/profit-chart")
    public ResponseEntity<ApiResponse<List<AdminRevenueChartPointResponse>>> profitChart(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "groupBy", defaultValue = "month") String groupBy
    ) {
        return responseEntity(true, "Profit chart loaded", HttpStatus.OK, buildProfitChart(from, to, groupBy));
    }

    private List<AdminRevenueChartPointResponse> buildProfitChart(LocalDate from, LocalDate to, String groupBy) {
        boolean byYear = "year".equalsIgnoreCase(groupBy.trim());

        ZonedDateTime nowPeriodUtc = ZonedDateTime.now(ZoneOffset.UTC);
        if (byYear) {
            nowPeriodUtc = nowPeriodUtc.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
        } else {
            nowPeriodUtc = nowPeriodUtc.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        }

        Instant start = (from != null
                ? from.atStartOfDay(ZoneOffset.UTC)
                : (byYear ? nowPeriodUtc.minusYears(4) : nowPeriodUtc.minusMonths(11))).toInstant();

        Instant endExclusive = (to != null
                ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC)
                : (byYear ? nowPeriodUtc.plusYears(1) : nowPeriodUtc.plusMonths(1))).toInstant();

        Map<Instant, PeriodTotals> dbByPeriod = new LinkedHashMap<>();
        List<Object[]> rows = byYear
                ? orderItemRepository.profitByYear(start, endExclusive)
                : orderItemRepository.profitByMonth(start, endExclusive);

        for (Object[] row : rows) {
            Instant period = byYear
                    ? normalizeToUtcYearStart(toInstant(row[0]))
                    : normalizeToUtcMonthStart(toInstant(row[0]));
            BigDecimal revenue = toBigDecimal(row[1]);
            BigDecimal cost = toBigDecimal(row[2]);
            dbByPeriod.put(period, new PeriodTotals(revenue, cost));
        }

        ZonedDateTime cursor = start.atZone(ZoneOffset.UTC);
        if (byYear) {
            cursor = cursor.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
        } else {
            cursor = cursor.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        }

        ZonedDateTime endPeriod = endExclusive.minusMillis(1).atZone(ZoneOffset.UTC);
        if (byYear) {
            endPeriod = endPeriod.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
        } else {
            endPeriod = endPeriod.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        }

        List<AdminRevenueChartPointResponse> points = new ArrayList<>();
        while (!cursor.isAfter(endPeriod)) {
            Instant periodStart = byYear
                    ? normalizeToUtcYearStart(cursor.toInstant())
                    : normalizeToUtcMonthStart(cursor.toInstant());
            PeriodTotals totals = dbByPeriod.getOrDefault(periodStart, PeriodTotals.ZERO);
            BigDecimal revenue = scale2(totals.revenue());
            BigDecimal cost = scale2(totals.cost());
            BigDecimal profit = scale2(revenue.subtract(cost));
            points.add(new AdminRevenueChartPointResponse(periodStart, revenue, cost, profit));
            cursor = byYear ? cursor.plusYears(1) : cursor.plusMonths(1);
        }
        return points;
    }

    private static double profitMarginPercent(BigDecimal revenue, BigDecimal profit) {
        if (revenue == null || revenue.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return roundPercent(profit
                .divide(revenue, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue());
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

    private static BigDecimal scale2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal b) {
            return b;
        }
        return BigDecimal.valueOf(((Number) value).doubleValue());
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

    private static Instant normalizeToUtcYearStart(Instant instant) {
        return instant.atZone(ZoneOffset.UTC)
                .withDayOfYear(1)
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
    }

    private record PeriodTotals(BigDecimal revenue, BigDecimal cost) {
        static final PeriodTotals ZERO = new PeriodTotals(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
