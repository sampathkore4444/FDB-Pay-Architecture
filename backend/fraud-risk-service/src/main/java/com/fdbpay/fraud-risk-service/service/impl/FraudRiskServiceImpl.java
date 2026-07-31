package com.fdbpay.fraud.risk.service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fdbpay.fraud.risk.service.dto.request.SanctionScreeningRequest;
import com.fdbpay.fraud.risk.service.dto.request.TransactionEvaluationRequest;
import com.fdbpay.fraud.risk.service.dto.response.AdminAmlAlertResponse;
import com.fdbpay.fraud.risk.service.dto.response.FraudAlertResponse;
import com.fdbpay.fraud.risk.service.dto.response.FraudEvaluationResponse;
import com.fdbpay.fraud.risk.service.model.FraudAlert;
import com.fdbpay.fraud.risk.service.model.enums.AlertSeverity;
import com.fdbpay.fraud.risk.service.model.enums.AlertStatus;
import com.fdbpay.fraud.risk.service.model.enums.AlertType;
import com.fdbpay.fraud.risk.service.repository.FraudAlertRepository;
import com.fdbpay.fraud.risk.service.service.FraudRiskService;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudRiskServiceImpl implements FraudRiskService {

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("20000000");
    private static final int VELOCITY_WINDOW_SECONDS = 60;
    private static final int VELOCITY_MAX_TXN = 10;
    private static final int HIGH_AMOUNT_RISK_SCORE = 40;
    private static final int VELOCITY_RISK_SCORE = 30;
    private static final int ANOMALY_RISK_SCORE = 20;
    private static final int SANCTIONS_RISK_SCORE = 60;
    private static final int RISK_THRESHOLD = 70;

    private final FraudAlertRepository fraudAlertRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public FraudEvaluationResponse evaluateTransaction(TransactionEvaluationRequest request) {
        log.info("Evaluating transaction: {}", request.getTransactionId());

        int riskScore = 0;
        List<String> reasons = new ArrayList<>();

        if (request.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            riskScore += HIGH_AMOUNT_RISK_SCORE;
            reasons.add("Transaction amount " + request.getAmount() + " exceeds high-value threshold of " + HIGH_AMOUNT_THRESHOLD);
        }

        String velocityKey = "fraud:velocity:" + request.getSenderUserId();
        Long currentCount = redisTemplate.opsForValue().increment(velocityKey);
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(velocityKey, VELOCITY_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (currentCount != null && currentCount > VELOCITY_MAX_TXN) {
            riskScore += VELOCITY_RISK_SCORE;
            reasons.add("User " + request.getSenderUserId() + " exceeded velocity limit of " + VELOCITY_MAX_TXN + " transactions in " + VELOCITY_WINDOW_SECONDS + "s window");
        }

        boolean isSelfTransfer = request.getSenderUserId().equals(request.getReceiverUserId());
        if (isSelfTransfer) {
            riskScore += ANOMALY_RISK_SCORE;
            reasons.add("Self-transfer detected: sender and receiver are the same user");
        }

        riskScore = Math.min(riskScore, 100);

        boolean approved = riskScore < RISK_THRESHOLD;

        if (!approved) {
            createFraudAlert(request, riskScore, reasons);
        }

        return FraudEvaluationResponse.builder()
                .approved(approved)
                .riskScore(riskScore)
                .reasons(reasons)
                .build();
    }

    @Override
    public boolean screenSanctions(SanctionScreeningRequest request) {
        log.info("Screening sanctions for: {}", request.getName());

        List<String> knownSanctionedNames = List.of(
                "OFAC_MATCH",
                "UN_SANCTIONS_MATCH",
                "EU_SANCTIONS_MATCH",
                "PEP_MATCH"
        );

        String normalizedName = request.getName().toLowerCase().trim();
        for (String sanctionedPattern : knownSanctionedNames) {
            if (normalizedName.contains(sanctionedPattern.toLowerCase().replace("_", " "))) {
                log.warn("Sanctions match found for: {}", request.getName());
                return true;
            }
        }

        if (request.getNrcNumber() != null && !request.getNrcNumber().isBlank()) {
            String sanitizedNrc = request.getNrcNumber().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            if (sanitizedNrc.length() < 5) {
                log.warn("Suspiciously short NRC number provided for: {}", request.getName());
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FraudAlertResponse> getAlerts(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FraudAlert> alerts = fraudAlertRepository.findAll(pageRequest);
        return alerts.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public FraudAlertResponse resolveAlert(UUID alertId, AlertStatus status) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudAlert", alertId.toString()));

        alert.setStatus(status);
        if (status == AlertStatus.RESOLVED || status == AlertStatus.FALSE_POSITIVE) {
            alert.setResolvedAt(OffsetDateTime.now());
        }

        FraudAlert saved = fraudAlertRepository.save(alert);
        log.info("Alert {} resolved with status {}", alertId, status);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminAmlAlertResponse> getAmlAlerts(AlertSeverity severity, AlertStatus status, int page, int size) {
        List<FraudAlert> alerts = fraudAlertRepository.findAll().stream()
                .filter(a -> severity == null || a.getSeverity() == severity)
                .filter(a -> status == null || a.getStatus() == status)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

        return alerts.stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::mapToAdminAmlResponse)
                .toList();
    }

    @Override
    @Transactional
    public AdminAmlAlertResponse actionAlert(UUID alertId, String action, String reason) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudAlert", alertId.toString()));

        AlertStatus newStatus;
        switch (action.toUpperCase()) {
            case "DISMISS" -> newStatus = AlertStatus.FALSE_POSITIVE;
            case "INVESTIGATE" -> newStatus = AlertStatus.INVESTIGATING;
            case "RESOLVE" -> newStatus = AlertStatus.RESOLVED;
            default -> throw new BusinessException("INVALID_ACTION", "Unsupported alert action: " + action);
        }

        alert.setStatus(newStatus);
        if (newStatus == AlertStatus.RESOLVED || newStatus == AlertStatus.FALSE_POSITIVE) {
            alert.setResolvedAt(OffsetDateTime.now());
        }
        if (reason != null && !reason.isBlank()) {
            String existing = alert.getDetails();
            alert.setDetails(existing != null && !existing.isBlank() ? existing + " | " + reason : reason);
        }

        FraudAlert saved = fraudAlertRepository.save(alert);
        log.info("Alert {} action {} applied, status={}", alertId, action, newStatus);
        return mapToAdminAmlResponse(saved);
    }

    private void createFraudAlert(TransactionEvaluationRequest request, int riskScore, List<String> reasons) {
        AlertSeverity severity;
        if (riskScore >= 90) {
            severity = AlertSeverity.CRITICAL;
        } else if (riskScore >= 70) {
            severity = AlertSeverity.HIGH;
        } else if (riskScore >= 50) {
            severity = AlertSeverity.MEDIUM;
        } else {
            severity = AlertSeverity.LOW;
        }

        AlertType alertType;
        if (riskScore >= HIGH_AMOUNT_RISK_SCORE && request.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            alertType = AlertType.HIGH_AMOUNT;
        } else if (reasons.stream().anyMatch(r -> r.contains("velocity limit"))) {
            alertType = AlertType.VELOCITY;
        } else {
            alertType = AlertType.ANOMALY;
        }

        String details;
        try {
            details = objectMapper.writeValueAsString(List.of(
                    "transactionId", request.getTransactionId().toString(),
                    "riskScore", String.valueOf(riskScore),
                    "reasons", String.join("; ", reasons)
            ));
        } catch (Exception e) {
            details = reasons.toString();
        }

        FraudAlert alert = FraudAlert.builder()
                .transactionId(request.getTransactionId())
                .userId(request.getSenderUserId())
                .alertType(alertType)
                .severity(severity)
                .status(AlertStatus.OPEN)
                .details(details)
                .createdAt(OffsetDateTime.now())
                .build();

        fraudAlertRepository.save(alert);
        log.warn("Fraud alert created for transaction {} with risk score {}", request.getTransactionId(), riskScore);
    }

    private FraudAlertResponse mapToResponse(FraudAlert alert) {
        return FraudAlertResponse.builder()
                .id(alert.getId())
                .transactionId(alert.getTransactionId())
                .userId(alert.getUserId())
                .alertType(alert.getAlertType())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .details(alert.getDetails())
                .createdAt(alert.getCreatedAt())
                .resolvedAt(alert.getResolvedAt())
                .build();
    }

    private AdminAmlAlertResponse mapToAdminAmlResponse(FraudAlert alert) {
        return AdminAmlAlertResponse.builder()
                .id(alert.getId())
                .userId(alert.getUserId())
                .type(alert.getAlertType().name())
                .severity(alert.getSeverity().name())
                .status(alert.getStatus().name())
                .description(alert.getDetails())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
