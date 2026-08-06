package com.fdbpay.transfer.service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fdbpay.transfer.service.dto.response.analytics.AnalyticsTransactionRow;
import com.fdbpay.transfer.service.dto.response.analytics.CustomerInsight;
import com.fdbpay.transfer.service.dto.response.analytics.MerchantAnalyticsBenchmark;
import com.fdbpay.transfer.service.dto.response.analytics.MerchantAnalyticsSummary;
import com.fdbpay.transfer.service.dto.response.analytics.StorePerformance;
import com.fdbpay.transfer.service.model.Transaction;
import com.fdbpay.transfer.service.model.enums.TransactionStatus;
import com.fdbpay.transfer.service.repository.TransactionRepository;
import com.fdbpay.transfer.service.service.MerchantAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantAnalyticsServiceImpl implements MerchantAnalyticsService {

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public MerchantAnalyticsSummary getSummary(UUID walletId, LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(startDate, endDate);

        List<Transaction> incoming = transactionRepository
                .findByReceiverWalletIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                        walletId, TransactionStatus.COMPLETED, range.from, range.to);
        List<Transaction> outgoing = transactionRepository
                .findBySenderWalletIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                        walletId, TransactionStatus.COMPLETED, range.from, range.to);

        long totalSales = incoming.stream().mapToLong(Transaction::getAmount).sum();
        long refundAmount = outgoing.stream().mapToLong(Transaction::getAmount).sum();
        long avg = incoming.isEmpty() ? 0L : totalSales / incoming.size();

        Map<String, MerchantAnalyticsSummary.PaymentMethodBreakdown> byMethod = new LinkedHashMap<>();
        for (Transaction tx : incoming) {
            String method = resolveMethod(tx);
            byMethod.computeIfAbsent(method, m -> MerchantAnalyticsSummary.PaymentMethodBreakdown.builder()
                            .method(m).count(0).amount(0L).build());
            byMethod.get(method).setCount(byMethod.get(method).getCount() + 1);
            byMethod.get(method).setAmount(byMethod.get(method).getAmount() + tx.getAmount());
        }

        Map<LocalDate, MerchantAnalyticsSummary.DailyPoint> byDay = new TreeMap<>();
        for (Transaction tx : incoming) {
            LocalDate day = tx.getCreatedAt().toLocalDate();
            byDay.computeIfAbsent(day, d -> MerchantAnalyticsSummary.DailyPoint.builder()
                            .date(d.toString()).count(0).amount(0L).build());
            byDay.get(day).setCount(byDay.get(day).getCount() + 1);
            byDay.get(day).setAmount(byDay.get(day).getAmount() + tx.getAmount());
        }

        Map<UUID, MerchantAnalyticsSummary.TopCustomer> customers = new LinkedHashMap<>();
        for (Transaction tx : incoming) {
            UUID wallet = tx.getSenderWalletId();
            customers.computeIfAbsent(wallet, w -> MerchantAnalyticsSummary.TopCustomer.builder()
                            .counterpartyWalletId(w.toString()).count(0).amount(0L).build());
            customers.get(wallet).setCount(customers.get(wallet).getCount() + 1);
            customers.get(wallet).setAmount(customers.get(wallet).getAmount() + tx.getAmount());
        }
        List<MerchantAnalyticsSummary.TopCustomer> topCustomers = customers.values().stream()
                .sorted(Comparator.comparingLong(MerchantAnalyticsSummary.TopCustomer::getAmount).reversed())
                .limit(5)
                .toList();

        return MerchantAnalyticsSummary.builder()
                .totalSales(totalSales)
                .saleCount(incoming.size())
                .avgTransactionValue(avg)
                .refundCount(outgoing.size())
                .refundAmount(refundAmount)
                .netSales(totalSales - refundAmount)
                .paymentMethods(new ArrayList<>(byMethod.values()))
                .dailySeries(new ArrayList<>(byDay.values()))
                .topCustomers(topCustomers)
                .build();
    }

    @Override
    public MerchantAnalyticsBenchmark getBenchmark(UUID walletId, LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(startDate, endDate);

        List<Transaction> merchantSales = transactionRepository
                .findByReceiverWalletIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                        walletId, TransactionStatus.COMPLETED, range.from, range.to);
        List<Transaction> platform = transactionRepository
                .findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(TransactionStatus.COMPLETED, range.from, range.to);

        long merchantSalesAmount = merchantSales.stream().mapToLong(Transaction::getAmount).sum();
        long merchantAvg = merchantSales.isEmpty() ? 0L : merchantSalesAmount / merchantSales.size();

        long platformAmount = platform.stream().mapToLong(Transaction::getAmount).sum();
        long platformAvg = platform.isEmpty() ? 0L : platformAmount / platform.size();

        Double vsPercent = platformAvg > 0
                ? Math.round(((merchantAvg - platformAvg) / (double) platformAvg) * 1000.0) / 10.0
                : null;

        return MerchantAnalyticsBenchmark.builder()
                .merchantTotalSales(merchantSalesAmount)
                .merchantSaleCount(merchantSales.size())
                .merchantAvgTransactionValue(merchantAvg)
                .platformTotalSales(platformAmount)
                .platformTransactionCount(platform.size())
                .platformAvgTransactionValue(platformAvg)
                .vsAveragePercent(vsPercent)
                .build();
    }

    @Override
    public Page<AnalyticsTransactionRow> getTransactions(
            UUID walletId, LocalDate startDate, LocalDate endDate,
            String direction, Long minAmount, Long maxAmount, String method,
            String terminalId, String staffId, int page, int size) {
        DateRange range = resolveRange(startDate, endDate);

        List<AnalyticsTransactionRow> rows = new ArrayList<>();

        boolean includeSales = direction == null || direction.isBlank() || "SALE".equalsIgnoreCase(direction);
        boolean includeRefunds = direction == null || direction.isBlank() || "REFUND".equalsIgnoreCase(direction);

        if (includeSales) {
            transactionRepository
                    .findByReceiverWalletIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                            walletId, TransactionStatus.COMPLETED, range.from, range.to)
                    .forEach(tx -> rows.add(toRow(tx, "SALE")));
        }
        if (includeRefunds) {
            transactionRepository
                    .findBySenderWalletIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                            walletId, TransactionStatus.COMPLETED, range.from, range.to)
                    .forEach(tx -> rows.add(toRow(tx, "REFUND")));
        }

        List<AnalyticsTransactionRow> filtered = rows.stream()
                .filter(r -> minAmount == null || r.getAmount() >= minAmount)
                .filter(r -> maxAmount == null || r.getAmount() <= maxAmount)
                .filter(r -> method == null || method.isBlank() || method.equalsIgnoreCase(r.getMethod()))
                .filter(r -> matchesMetadata(r, "terminalId", terminalId))
                .filter(r -> matchesMetadata(r, "staffId", staffId))
                .sorted(Comparator.comparing(AnalyticsTransactionRow::getCreatedAt).reversed())
                .toList();

        int total = filtered.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<AnalyticsTransactionRow> content = filtered.isEmpty() ? List.of() : filtered.subList(from, to);
        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    @Override
    public List<CustomerInsight> getCustomers(UUID walletId) {
        DateRange range = resolveRange(null, null);
        List<Transaction> incoming = transactionRepository
                .findByReceiverWalletIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                        walletId, TransactionStatus.COMPLETED, range.from, range.to);

        Map<String, CustomerInsight> byCustomer = new LinkedHashMap<>();
        for (Transaction tx : incoming) {
            String wallet = String.valueOf(tx.getSenderWalletId());
            CustomerInsight insight = byCustomer.computeIfAbsent(wallet, w -> CustomerInsight.builder()
                    .walletId(w).count(0).amount(0L).build());
            insight.setCount(insight.getCount() + 1);
            insight.setAmount(insight.getAmount() + tx.getAmount());
            if (insight.getLastPurchaseAt() == null || tx.getCreatedAt().isAfter(insight.getLastPurchaseAt())) {
                insight.setLastPurchaseAt(tx.getCreatedAt());
            }
        }
        return byCustomer.values().stream()
                .sorted(Comparator.comparingLong(CustomerInsight::getAmount).reversed())
                .toList();
    }

    @Override
    public List<StorePerformance> getStorePerformance(UUID walletId) {
        DateRange range = resolveRange(null, null);
        List<Transaction> incoming = transactionRepository
                .findByReceiverWalletIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                        walletId, TransactionStatus.COMPLETED, range.from, range.to);

        Map<String, StorePerformance> byStore = new LinkedHashMap<>();
        for (Transaction tx : incoming) {
            String storeId = extractMetadataString(tx, "storeId");
            if (storeId == null || storeId.isBlank()) {
                storeId = "UNASSIGNED";
            }
            StorePerformance perf = byStore.computeIfAbsent(storeId, s -> StorePerformance.builder()
                    .storeId(s).count(0).amount(0L).build());
            perf.setCount(perf.getCount() + 1);
            perf.setAmount(perf.getAmount() + tx.getAmount());
        }
        return byStore.values().stream()
                .sorted(Comparator.comparingLong(StorePerformance::getAmount).reversed())
                .toList();
    }

    private String extractMetadataString(Transaction tx, String key) {
        JsonNode node = parseMetadata(tx);
        if (node != null && node.hasNonNull(key)) {
            return node.get(key).asText();
        }
        return null;
    }

    private AnalyticsTransactionRow toRow(Transaction tx, String direction) {
        return AnalyticsTransactionRow.builder()
                .id(tx.getId())
                .direction(direction)
                .type(tx.getType().name())
                .method(resolveMethod(tx))
                .amount(tx.getAmount())
                .fee(tx.getFee())
                .description(tx.getDescription())
                .senderWalletId(tx.getSenderWalletId())
                .receiverWalletId(tx.getReceiverWalletId())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private String resolveMethod(Transaction tx) {
        JsonNode node = parseMetadata(tx);
        if (node != null && node.hasNonNull("method") && !node.get("method").asText().isBlank()) {
            return node.get("method").asText();
        }
        return tx.getType().name();
    }

    private boolean matchesMetadata(AnalyticsTransactionRow row, String key, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        Transaction tx = transactionRepository.findById(row.getId()).orElse(null);
        if (tx == null) {
            return false;
        }
        JsonNode node = parseMetadata(tx);
        return node != null && node.hasNonNull(key) && expected.equals(node.get(key).asText());
    }

    private JsonNode parseMetadata(Transaction tx) {
        if (tx.getMetadata() == null || tx.getMetadata().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(tx.getMetadata());
        } catch (Exception e) {
            return null;
        }
    }

    private DateRange resolveRange(LocalDate startDate, LocalDate endDate) {
        LocalDate from = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate to = endDate != null ? endDate : LocalDate.now();
        return new DateRange(from.atStartOfDay().atOffset(java.time.ZoneOffset.UTC),
                to.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC).minusNanos(1));
    }

    private record DateRange(OffsetDateTime from, OffsetDateTime to) {
    }
}
