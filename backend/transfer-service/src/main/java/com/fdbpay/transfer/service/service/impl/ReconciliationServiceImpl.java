package com.fdbpay.transfer.service.service.impl;

import com.fdbpay.transfer.service.dto.response.ReconciliationRow;
import com.fdbpay.transfer.service.model.Transaction;
import com.fdbpay.transfer.service.model.enums.TransactionStatus;
import com.fdbpay.transfer.service.repository.TransactionRepository;
import com.fdbpay.transfer.service.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private static final String SETTLEMENT_SERVICE_BASE = "http://settlement-service/settlements";

    private final TransactionRepository transactionRepository;
    private final WebClient.Builder webClientBuilder;

    @Override
    public List<ReconciliationRow> reconcile(UUID walletId, UUID merchantId, LocalDate from, LocalDate to) {
        OffsetDateTime fromTime = from.atStartOfDay().toInstant(ZoneOffset.UTC).atOffset(ZoneOffset.UTC);
        OffsetDateTime toTime = to.atTime(23, 59, 59).toInstant(ZoneOffset.UTC).atOffset(ZoneOffset.UTC);

        List<Transaction> sales = transactionRepository
                .findByReceiverWalletIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(walletId, TransactionStatus.COMPLETED, fromTime, toTime);
        List<Transaction> refunds = transactionRepository
                .findBySenderWalletIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(walletId, TransactionStatus.COMPLETED, fromTime, toTime);

        List<Map<String, Object>> settlements = fetchSettlements(merchantId);

        List<ReconciliationRow> rows = new ArrayList<>();
        LocalDate day = from;
        while (!day.isAfter(to)) {
            final LocalDate d = day;
            List<Transaction> daySales = sales.stream()
                    .filter(t -> t.getCreatedAt().toLocalDate().equals(d)).toList();
            List<Transaction> dayRefunds = refunds.stream()
                    .filter(t -> t.getCreatedAt().toLocalDate().equals(d)).toList();

            long gross = daySales.stream().mapToLong(Transaction::getAmount).sum();
            long fees = daySales.stream().mapToLong(Transaction::getFee).sum();
            long refundAmount = dayRefunds.stream().mapToLong(Transaction::getAmount).sum();

            Map<String, Object> settlement = matchSettlement(settlements, d, gross);
            boolean matched = settlement != null;

            rows.add(ReconciliationRow.builder()
                    .date(d)
                    .grossSales(gross)
                    .saleCount(daySales.size())
                    .refundAmount(refundAmount)
                    .refundCount(dayRefunds.size())
                    .fees(fees)
                    .netSales(gross - fees - refundAmount)
                    .settlementRef(matched ? String.valueOf(settlement.get("settlementRef")) : null)
                    .settledAt(matched ? parseOffset(settlement.get("settledAt")) : null)
                    .status(matched ? "MATCHED" : (gross == 0 ? "NO_ACTIVITY" : "UNMATCHED"))
                    .build());
            day = day.plusDays(1);
        }
        return rows.stream().sorted(Comparator.comparing(ReconciliationRow::getDate)).toList();
    }

    private Map<String, Object> matchSettlement(List<Map<String, Object>> settlements, LocalDate day, long gross) {
        for (Map<String, Object> s : settlements) {
            LocalDate sDate = parseDate(s.get("periodStart"));
            LocalDate eDate = parseDate(s.get("periodEnd"));
            if (sDate == null || eDate == null) continue;
            if (!day.isBefore(sDate) && !day.isAfter(eDate)) {
                if (gross == 0 || safeLong(s.get("grossAmount")) == gross) {
                    return s;
                }
            }
        }
        return null;
    }

    private OffsetDateTime parseOffset(Object value) {
        if (value == null) return null;
        try {
            return OffsetDateTime.parse(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(Object value) {
        if (value == null) return null;
        try {
            return java.time.OffsetDateTime.parse(String.valueOf(value)).toLocalDate();
        } catch (Exception e) {
            try {
                return LocalDate.parse(String.valueOf(value));
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private long safeLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private List<Map<String, Object>> fetchSettlements(UUID merchantId) {
        try {
            Map<?, ?> body = webClientBuilder.build()
                    .get()
                    .uri(uri -> uri.scheme("http").host("settlement-service").path("/settlements/merchant/{id}")
                            .queryParam("size", 500).build(merchantId))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (body == null) return List.of();
            Object data = body.get("data");
            if (data instanceof Map<?, ?> page && page.get("content") instanceof List<?> content) {
                return content.stream()
                        .filter(c -> c instanceof Map<?, ?>)
                        .map(c -> (Map<String, Object>) c)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch settlements for merchant {}: {}", merchantId, e.getMessage());
        }
        return List.of();
    }

    @Override
    public String toCsv(List<ReconciliationRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("date,grossSales,saleCount,refundAmount,refundCount,fees,netSales,settlementRef,settledAt,status\n");
        for (ReconciliationRow r : rows) {
            sb.append(r.getDate()).append(',')
                    .append(r.getGrossSales()).append(',')
                    .append(r.getSaleCount()).append(',')
                    .append(r.getRefundAmount()).append(',')
                    .append(r.getRefundCount()).append(',')
                    .append(r.getFees()).append(',')
                    .append(r.getNetSales()).append(',')
                    .append(r.getSettlementRef() == null ? "" : r.getSettlementRef()).append(',')
                    .append(r.getSettledAt() == null ? "" : r.getSettledAt()).append(',')
                    .append(r.getStatus()).append('\n');
        }
        return sb.toString();
    }
}
