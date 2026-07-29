package com.fdbpay.remittance.service.service.impl;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.event.NotificationEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.remittance.service.dto.request.InitiateRemittanceRequest;
import com.fdbpay.remittance.service.dto.request.RemittanceWebhookRequest;
import com.fdbpay.remittance.service.dto.response.RemittanceCorridorResponse;
import com.fdbpay.remittance.service.dto.response.RemittanceRateQuoteResponse;
import com.fdbpay.remittance.service.dto.response.RemittanceResponse;
import com.fdbpay.remittance.service.model.Remittance;
import com.fdbpay.remittance.service.model.RemittanceCorridor;
import com.fdbpay.remittance.service.model.enums.RemittanceCorridorStatus;
import com.fdbpay.remittance.service.model.enums.RemittanceStatus;
import com.fdbpay.remittance.service.repository.RemittanceCorridorRepository;
import com.fdbpay.remittance.service.repository.RemittanceRepository;
import com.fdbpay.remittance.service.service.RemittanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemittanceServiceImpl implements RemittanceService {

    private final RemittanceRepository remittanceRepository;
    private final RemittanceCorridorRepository corridorRepository;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final WebClient.Builder webClientBuilder;

    private static final String WALLET_SERVICE_BASE = "http://wallet-service/wallet";
    private static final String TOPIC_NOTIFICATION = "notification.send";
    private static final String TOPIC_REMITTANCE_RECEIVED = "remittance.received";

    @Override
    public List<RemittanceCorridorResponse> getCorridors() {
        return corridorRepository.findByStatus(RemittanceCorridorStatus.ACTIVE)
                .stream()
                .map(this::mapCorridorToResponse)
                .toList();
    }

    @Override
    public RemittanceRateQuoteResponse getRateQuote(String corridorCode, Long amount) {
        RemittanceCorridor corridor = corridorRepository.findByCode(corridorCode)
                .orElseThrow(() -> new ResourceNotFoundException("Corridor", corridorCode));

        if (corridor.getStatus() != RemittanceCorridorStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Corridor is not active: " + corridorCode);
        }

        if (amount < corridor.getMinAmount() || amount > corridor.getMaxAmount()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Amount must be between " + corridor.getMinAmount() + " and " + corridor.getMaxAmount());
        }

        long fee = calculateFee(amount, corridor);
        long amountMmk = BigDecimal.valueOf(amount).multiply(corridor.getExchangeRate()).longValue() - fee;

        return RemittanceRateQuoteResponse.builder()
                .corridor(corridorCode)
                .amount(amount)
                .exchangeRate(corridor.getExchangeRate())
                .fee(fee)
                .amountMmk(amountMmk)
                .build();
    }

    @Override
    @Transactional
    public RemittanceResponse initiateRemittance(UUID userId, InitiateRemittanceRequest request) {
        RemittanceCorridor corridor = corridorRepository.findByCode(request.getCorridor())
                .orElseThrow(() -> new ResourceNotFoundException("Corridor", request.getCorridor()));

        if (corridor.getStatus() != RemittanceCorridorStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Corridor is not active: " + request.getCorridor());
        }

        if (request.getAmount() < corridor.getMinAmount() || request.getAmount() > corridor.getMaxAmount()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Amount must be between " + corridor.getMinAmount() + " and " + corridor.getMaxAmount());
        }

        long fee = calculateFee(request.getAmount(), corridor);
        long amountMmk = request.getAmount().multiply(corridor.getExchangeRate()).longValue() - fee;
        String referenceNumber = generateReferenceNumber();

        Remittance remittance = Remittance.builder()
                .recipientUserId(userId)
                .recipientPhone(request.getRecipientPhone())
                .senderName(request.getSenderName())
                .senderCountry(request.getSenderCountry())
                .corridor(request.getCorridor())
                .partnerRef(request.getPartnerRef() != null ? request.getPartnerRef() : referenceNumber)
                .amount(request.getAmount())
                .fee(fee)
                .exchangeRate(corridor.getExchangeRate())
                .amountMmk(amountMmk)
                .status(RemittanceStatus.PENDING)
                .referenceNumber(referenceNumber)
                .build();

        remittance = remittanceRepository.save(remittance);
        log.info("Remittance initiated: id={}, corridor={}, amount={}", remittance.getId(), request.getCorridor(), request.getAmount());

        try {
            simulatePartnerApiCall(corridor, remittance);
            remittance.setStatus(RemittanceStatus.COMPLETED);
            remittance.setReceivedAt(OffsetDateTime.now());
            remittance = remittanceRepository.save(remittance);

            creditRecipientWallet(remittance);
            publishRemittanceReceivedEvent(remittance);

            log.info("Remittance completed: id={}, amountMmk={}", remittance.getId(), remittance.getAmountMmk());
        } catch (Exception e) {
            log.error("Remittance processing failed: id={}", remittance.getId(), e);
            remittance.setStatus(RemittanceStatus.FAILED);
            remittance = remittanceRepository.save(remittance);
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR, "Remittance processing failed: " + e.getMessage());
        }

        return mapToResponse(remittance);
    }

    @Override
    public RemittanceResponse getRemittance(UUID id) {
        Remittance remittance = remittanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Remittance", id.toString()));
        return mapToResponse(remittance);
    }

    @Override
    public Page<RemittanceResponse> getMyRemittances(UUID userId, int page, int size) {
        Page<Remittance> remittances = remittanceRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return remittances.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public RemittanceResponse processWebhook(RemittanceWebhookRequest request) {
        Remittance remittance = remittanceRepository.findByPartnerRef(request.getPartnerRef())
                .orElseThrow(() -> new ResourceNotFoundException("Remittance", request.getPartnerRef()));

        RemittanceStatus newStatus = mapWebhookStatus(request.getStatus());
        remittance.setStatus(newStatus);

        if (newStatus == RemittanceStatus.COMPLETED) {
            remittance.setReceivedAt(OffsetDateTime.now());
            if (request.getReferenceNumber() != null) {
                remittance.setReferenceNumber(request.getReferenceNumber());
            }
            creditRecipientWallet(remittance);
            publishRemittanceReceivedEvent(remittance);
        }

        remittance = remittanceRepository.save(remittance);
        log.info("Remittance webhook processed: id={}, status={}", remittance.getId(), newStatus);
        return mapToResponse(remittance);
    }

    @Override
    @Transactional
    public RemittanceResponse handleCallback(RemittanceWebhookRequest request) {
        return processWebhook(request);
    }

    private long calculateFee(Long amount, RemittanceCorridor corridor) {
        long fixedFee = corridor.getFeeFixed();
        long percentageFee = amount.multiply(corridor.getFeePercentage())
                .setScale(0, RoundingMode.HALF_UP).longValue();
        return fixedFee + percentageFee;
    }

    private void simulatePartnerApiCall(RemittanceCorridor corridor, Remittance remittance) {
        log.info("Simulating partner API call to {} for remittance {}", corridor.getPartnerName(), remittance.getId());
    }

    private void creditRecipientWallet(Remittance remittance) {
        try {
            WebClient webClient = webClientBuilder.build();
            webClient.post()
                    .uri(WALLET_SERVICE_BASE + "/credit")
                    .bodyValue(java.util.Map.of(
                            "userId", remittance.getRecipientUserId().toString(),
                            "amount", remittance.getAmountMmk(),
                            "description", "Remittance received from " + remittance.getSenderName(),
                            "txnId", remittance.getId().toString()
                    ))
                    .retrieve()
                    .bodyToMono(java.util.Map.class)
                    .block();
            log.info("Credited wallet for remittance: id={}, userId={}, amount={}",
                    remittance.getId(), remittance.getRecipientUserId(), remittance.getAmountMmk());
        } catch (Exception e) {
            log.error("Failed to credit wallet for remittance: id={}", remittance.getId(), e);
        }
    }

    private void publishRemittanceReceivedEvent(Remittance remittance) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .userId(remittance.getRecipientUserId())
                    .channel("SMS")
                    .type("REMITTANCE_RECEIVED")
                    .title("Remittance Received")
                    .body(String.format("You have received %s MMK from %s via %s corridor.",
                            remittance.getAmountMmk(), remittance.getSenderName(), remittance.getCorridor()))
                    .timestamp(OffsetDateTime.now())
                    .build();
            kafkaTemplate.send(TOPIC_REMITTANCE_RECEIVED, remittance.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish remittance received event: id={}", remittance.getId(), e);
        }
    }

    private RemittanceStatus mapWebhookStatus(String status) {
        if (status == null) return RemittanceStatus.PENDING;
        return switch (status.toUpperCase()) {
            case "COMPLETED", "SUCCESS" -> RemittanceStatus.COMPLETED;
            case "FAILED", "REJECTED" -> RemittanceStatus.FAILED;
            case "REFUNDED" -> RemittanceStatus.REFUNDED;
            default -> RemittanceStatus.PENDING;
        };
    }

    private String generateReferenceNumber() {
        return "RM" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private RemittanceResponse mapToResponse(Remittance remittance) {
        return RemittanceResponse.builder()
                .id(remittance.getId())
                .recipientUserId(remittance.getRecipientUserId())
                .recipientPhone(remittance.getRecipientPhone())
                .senderName(remittance.getSenderName())
                .senderCountry(remittance.getSenderCountry())
                .corridor(remittance.getCorridor())
                .partnerRef(remittance.getPartnerRef())
                .amount(remittance.getAmount())
                .fee(remittance.getFee())
                .exchangeRate(remittance.getExchangeRate())
                .amountMmk(remittance.getAmountMmk())
                .status(remittance.getStatus())
                .referenceNumber(remittance.getReferenceNumber())
                .receivedAt(remittance.getReceivedAt())
                .createdAt(remittance.getCreatedAt())
                .build();
    }

    private RemittanceCorridorResponse mapCorridorToResponse(RemittanceCorridor corridor) {
        return RemittanceCorridorResponse.builder()
                .id(corridor.getId())
                .code(corridor.getCode())
                .sourceCountry(corridor.getSourceCountry())
                .sourceCurrency(corridor.getSourceCurrency())
                .destCurrency(corridor.getDestCurrency())
                .exchangeRate(corridor.getExchangeRate())
                .feeFixed(corridor.getFeeFixed())
                .feePercentage(corridor.getFeePercentage())
                .minAmount(corridor.getMinAmount())
                .maxAmount(corridor.getMaxAmount())
                .partnerName(corridor.getPartnerName())
                .status(corridor.getStatus())
                .createdAt(corridor.getCreatedAt())
                .build();
    }
}
