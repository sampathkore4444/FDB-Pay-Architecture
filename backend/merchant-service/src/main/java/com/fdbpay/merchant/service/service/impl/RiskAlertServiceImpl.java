package com.fdbpay.merchant.service.service.impl;

import com.fdbpay.merchant.service.client.TransferAnalyticsClient;
import com.fdbpay.merchant.service.client.TransferServiceClient;
import com.fdbpay.merchant.service.dto.response.RiskAlertResponse;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.RiskAlert;
import com.fdbpay.merchant.service.model.enums.RiskAlertStatus;
import com.fdbpay.merchant.service.model.enums.RiskSeverity;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.RiskAlertRepository;
import com.fdbpay.merchant.service.service.RiskAlertService;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAlertServiceImpl implements RiskAlertService {

    private static final double SURGE_THRESHOLD = 2.0;
    private static final long HIGH_DAILY_VOLUME = 5_000_000L;

    private final RiskAlertRepository alertRepository;
    private final MerchantRepository merchantRepository;
    private final TransferAnalyticsClient analyticsClient;
    private final TransferServiceClient transferServiceClient;

    @Override
    @Transactional
    public List<RiskAlertResponse> getAlerts(UUID merchantId, UUID walletId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        detectSurge(merchantId, walletId, merchant.getAlertDailySurgeThreshold());
        detectHighVolume(merchantId, walletId);
        detectLargeOrder(merchantId, walletId, merchant.getAlertLargeOrderThreshold());

        return alertRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public RiskAlertResponse acknowledge(UUID merchantId, UUID alertId) {
        RiskAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("RiskAlert", alertId.toString()));
        if (!alert.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Alert does not belong to this merchant");
        }
        alert.setStatus(RiskAlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(OffsetDateTime.now());
        alert = alertRepository.save(alert);
        return mapToResponse(alert);
    }

    private void detectSurge(UUID merchantId, UUID walletId, long minVolume) {
        LocalDate today = LocalDate.now();
        long todayVolume = volumeFor(analyticsClient.getSummary(walletId, today, today));
        long priorWeekVolume = volumeFor(analyticsClient.getSummary(walletId, today.minusDays(7), today.minusDays(1)));
        long dailyAvg = priorWeekVolume / 7;

        if (todayVolume >= minVolume && dailyAvg > 0 && todayVolume > dailyAvg * SURGE_THRESHOLD) {
            createIfOpen(merchantId, "SURGE", RiskSeverity.HIGH,
                    "Unusual transaction surge",
                    "Today's volume (" + todayVolume + " MMK) is " + Math.round((double) todayVolume / dailyAvg * 10) / 10.0
                            + "x your 7-day average (" + dailyAvg + " MMK). This may indicate fraud or duplicate activity.");
        }
    }

    private void detectLargeOrder(UUID merchantId, UUID walletId, Long threshold) {
        if (threshold == null || threshold <= 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        long maxAmount = 0L;
        for (Map<String, Object> row : transferServiceClient.getTransactions(walletId, 200)) {
            Object dir = row.get("direction");
            if (dir != null && "SALE".equalsIgnoreCase(String.valueOf(dir))) {
                try {
                    long amount = ((Number) row.get("amount")).longValue();
                    if (amount > maxAmount) {
                        maxAmount = amount;
                    }
                } catch (Exception e) {
                    // ignore malformed rows
                }
            }
        }
        if (maxAmount >= threshold) {
            createIfOpen(merchantId, "LARGE_ORDER", RiskSeverity.MEDIUM,
                    "Large order detected",
                    "A single order of " + maxAmount + " MMK exceeded your alert threshold of " + threshold + " MMK. Review the transaction to confirm it is legitimate.");
        }
    }

    private void detectHighVolume(UUID merchantId, UUID walletId) {
        LocalDate today = LocalDate.now();
        long priorWeekVolume = volumeFor(analyticsClient.getSummary(walletId, today.minusDays(7), today.minusDays(1)));
        if (priorWeekVolume >= HIGH_DAILY_VOLUME) {
            createIfOpen(merchantId, "HIGH_VOLUME", RiskSeverity.MEDIUM,
                    "High weekly volume",
                    "Weekly volume (" + priorWeekVolume + " MMK) exceeded the " + HIGH_DAILY_VOLUME + " MMK threshold.");
        }
    }

    private void createIfOpen(UUID merchantId, String type, RiskSeverity severity, String title, String message) {
        if (alertRepository.findTopByMerchantIdAndAlertTypeAndStatusOrderByCreatedAtDesc(
                merchantId, type, RiskAlertStatus.OPEN).isPresent()) {
            return;
        }
        alertRepository.save(RiskAlert.builder()
                .merchantId(merchantId)
                .alertType(type)
                .severity(severity)
                .title(title)
                .message(message)
                .status(RiskAlertStatus.OPEN)
                .build());
        log.info("Risk alert created: merchantId={}, type={}", merchantId, type);
    }

    private long volumeFor(Map<String, Object> summary) {
        try {
            Object data = summary.get("data");
            if (data instanceof Map<?, ?> inner && inner.get("totalSales") != null) {
                return ((Number) inner.get("totalSales")).longValue();
            }
        } catch (Exception e) {
            log.warn("Failed to read analytics volume: {}", e.getMessage());
        }
        return 0L;
    }

    private RiskAlertResponse mapToResponse(RiskAlert alert) {
        return RiskAlertResponse.builder()
                .id(alert.getId())
                .merchantId(alert.getMerchantId())
                .alertType(alert.getAlertType())
                .severity(alert.getSeverity())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .status(alert.getStatus())
                .createdAt(alert.getCreatedAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .build();
    }
}
