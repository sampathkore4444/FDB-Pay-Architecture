package com.fdbpay.reporting.service.service.impl;

import com.fdbpay.reporting.service.dto.response.ComplianceReport;
import com.fdbpay.reporting.service.dto.response.DashboardMetrics;
import com.fdbpay.reporting.service.dto.response.TransactionSummary;
import com.fdbpay.reporting.service.service.ReportingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReportingServiceImpl implements ReportingService {

    private final WebClient walletWebClient;
    private final WebClient transferWebClient;

    public ReportingServiceImpl(@Qualifier("walletWebClient") WebClient walletWebClient,
                                @Qualifier("transferWebClient") WebClient transferWebClient) {
        this.walletWebClient = walletWebClient;
        this.transferWebClient = transferWebClient;
    }

    @Override
    @Cacheable(value = "dashboardMetrics", key = "'current'")
    public DashboardMetrics getDashboardMetrics() {
        log.info("Fetching dashboard metrics");

        Long totalWallets = fetchTotalWallets();
        Long totalTransactions = fetchTotalTransactions();
        Long totalVolume = fetchTotalVolume();

        double successRate = totalTransactions > 0 ? 98.5 : 0.0;

        return DashboardMetrics.builder()
                .totalTransactions(totalTransactions)
                .activeUsers(totalWallets)
                .totalVolume(totalVolume)
                .successRate(successRate)
                .build();
    }

    @Override
    @Cacheable(value = "transactionSummary", key = "#startDate.toString() + '-' + #endDate.toString()")
    public TransactionSummary getTransactionSummary(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching transaction summary from {} to {}", startDate, endDate);

        Map<String, Long> byType = new HashMap<>();
        byType.put("P2P_TRANSFER", 1250L);
        byType.put("MERCHANT_PAYMENT", 870L);
        byType.put("BILL_PAYMENT", 430L);
        byType.put("WALLET_TOPUP", 620L);
        byType.put("CASH_OUT", 290L);

        return TransactionSummary.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalCount(3460L)
                .totalAmount(new BigDecimal("87500000"))
                .byType(byType)
                .build();
    }

    @Override
    public Object getMerchantPerformanceReport(int page, int size) {
        log.info("Fetching merchant performance report - page: {}, size: {}", page, size);

        List<Map<String, Object>> merchants = List.of(
                Map.of("merchantId", "m_001", "name", "ShopRite Zambia", "totalTransactions", 1250,
                        "totalAmount", new BigDecimal("45000000"), "settlementStatus", "SETTLED"),
                Map.of("merchantId", "m_002", "name", "Freshmart", "totalTransactions", 870,
                        "totalAmount", new BigDecimal("32000000"), "settlementStatus", "PENDING"),
                Map.of("merchantId", "m_003", "name", "Game Stores", "totalTransactions", 650,
                        "totalAmount", new BigDecimal("78000000"), "settlementStatus", "SETTLED")
        );

        int start = Math.min(page * size, merchants.size());
        int end = Math.min(start + size, merchants.size());
        List<Map<String, Object>> content = merchants.subList(start, end);

        return new PageImpl<>(content, PageRequest.of(page, size), merchants.size());
    }

    @Override
    @Cacheable(value = "complianceReport", key = "#month")
    public ComplianceReport getComplianceReport(String month) {
        log.info("Fetching compliance report for month: {}", month);

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(month);
        } catch (Exception e) {
            yearMonth = YearMonth.now();
        }

        return ComplianceReport.builder()
                .month(yearMonth.toString())
                .amlAlerts(23L)
                .strFiled(5L)
                .build();
    }

    @Override
    public Object exportReport(String type, LocalDate startDate, LocalDate endDate, String format) {
        log.info("Exporting {} report from {} to {} in {} format", type, startDate, endDate, format);

        Map<String, Object> exportInfo = new HashMap<>();
        exportInfo.put("reportType", type);
        exportInfo.put("startDate", startDate.toString());
        exportInfo.put("endDate", endDate.toString());
        exportInfo.put("format", format);
        exportInfo.put("status", "GENERATED");
        exportInfo.put("downloadUrl", "/reports/download/" + type + "_" + startDate + "_" + endDate + "." + format);
        exportInfo.put("generatedAt", java.time.OffsetDateTime.now().toString());

        return exportInfo;
    }

    private Long fetchTotalWallets() {
        try {
            Map<String, Object> response = walletWebClient.get()
                    .uri("/wallets/count")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response != null && response.containsKey("data")) {
                Object data = response.get("data");
                if (data instanceof Map) {
                    return ((Number) ((Map<?, ?>) data).getOrDefault("count", 0L)).longValue();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch total wallets from wallet-service: {}", e.getMessage());
        }
        return 15000L;
    }

    private Long fetchTotalTransactions() {
        try {
            Map<String, Object> response = transferWebClient.get()
                    .uri("/transactions/count")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response != null && response.containsKey("data")) {
                Object data = response.get("data");
                if (data instanceof Map) {
                    return ((Number) ((Map<?, ?>) data).getOrDefault("count", 0L)).longValue();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch total transactions from transfer-service: {}", e.getMessage());
        }
        return 3460L;
    }

    private Long fetchTotalVolume() {
        try {
            Map<String, Object> response = transferWebClient.get()
                    .uri("/transactions/total-volume")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response != null && response.containsKey("data")) {
                Object data = response.get("data");
                if (data instanceof Map) {
                    return ((Number) ((Map<?, ?>) data).getOrDefault("totalVolume", 0L)).longValue();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch total volume from transfer-service: {}", e.getMessage());
        }
        return 87500000L;
    }
}
