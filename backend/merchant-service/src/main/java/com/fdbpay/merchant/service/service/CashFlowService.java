package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.response.CashFlowForecastResponse;
import com.fdbpay.merchant.service.model.Order;
import com.fdbpay.merchant.service.model.enums.OrderStatus;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.OrderRepository;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashFlowService {

    private final OrderRepository orderRepository;
    private final MerchantRepository merchantRepository;

    public CashFlowForecastResponse forecast(UUID merchantId, int months) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        int horizon = Math.min(Math.max(months, 1), 24);
        YearMonth now = YearMonth.now();
        YearMonth start = now.minusMonths(5);

        List<CashFlowForecastResponse.MonthPoint> points = new ArrayList<>();
        Map<YearMonth, Long> revenueByMonth = new LinkedHashMap<>();
        for (Order order : orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)) {
            if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.PENDING) {
                continue;
            }
            YearMonth ym = YearMonth.from(order.getCreatedAt());
            if (ym.isBefore(start)) continue;
            revenueByMonth.merge(ym, order.getTotal(), Long::sum);
        }

        long[] lastSix = new long[6];
        for (int i = 0; i < 6; i++) {
            lastSix[i] = revenueByMonth.getOrDefault(now.minusMonths(5 - i), 0L);
        }
        double growth = 0;
        long denom = 0;
        for (int i = 1; i < 6; i++) {
            denom += lastSix[i - 1];
        }
        if (denom > 0) {
            growth = 5.0 * (lastSix[5] - lastSix[0]) / denom;
        }
        long monthlyAvg = 0;
        long sum = 0;
        for (long v : lastSix) sum += v;
        monthlyAvg = sum / 6;

        for (int i = 0; i < horizon; i++) {
            YearMonth ym = now.plusMonths(i);
            if (i < 5) {
                long actual = revenueByMonth.getOrDefault(start.plusMonths(i), 0L);
                long projection = Math.round(actual * (1 + growth));
                points.add(CashFlowForecastResponse.MonthPoint.builder()
                        .month(ym.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                        .revenue(actual)
                        .projection(projection)
                        .build());
            } else {
                long projected = Math.round(monthlyAvg * (1 + growth * (i - 4)));
                points.add(CashFlowForecastResponse.MonthPoint.builder()
                        .month(ym.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                        .revenue(0L)
                        .projection(projected)
                        .build());
            }
        }

        long projectedAnnual = 0;
        for (int i = 0; i < 12; i++) {
            projectedAnnual += Math.round(monthlyAvg * (1 + growth * i));
        }
        return CashFlowForecastResponse.builder()
                .months(points)
                .projectedAnnual(projectedAnnual)
                .averageMonthly(monthlyAvg)
                .growthRatePct((int) Math.round(growth * 100))
                .seasonalAdjustment(0L)
                .build();
    }
}
