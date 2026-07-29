package com.fdbpay.bill.service.service.impl;

import com.fdbpay.bill.service.dto.request.AirtimeTopupRequest;
import com.fdbpay.bill.service.dto.response.AirtimeTopupResponse;
import com.fdbpay.bill.service.model.AirtimeTopup;
import com.fdbpay.bill.service.model.enums.AirtimeProvider;
import com.fdbpay.bill.service.repository.AirtimeTopupRepository;
import com.fdbpay.bill.service.service.AirtimeTopupService;
import com.fdbpay.shared.constants.AppConstants;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.event.NotificationEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirtimeTopupServiceImpl implements AirtimeTopupService {

    private final AirtimeTopupRepository airtimeTopupRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String WALLET_SERVICE_BASE = "http://wallet-service/wallet";
    private static final String AIRTIME_COMPLETED_TOPIC = "airtime.completed";

    @Override
    @Transactional
    public AirtimeTopupResponse topup(UUID userId, AirtimeTopupRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        validateProvider(request.getProvider());

        AirtimeTopup topup = AirtimeTopup.builder()
                .userId(userId)
                .phone(request.getPhone())
                .amount(request.getAmount())
                .provider(request.getProvider())
                .status(AirtimeTopup.TopupStatus.PENDING)
                .build();

        topup = airtimeTopupRepository.save(topup);
        log.info("Airtime topup initiated: id={}, phone={}, provider={}, amount={}",
                topup.getId(), request.getPhone(), request.getProvider(), request.getAmount());

        try {
            processTopup(topup);
        } catch (Exception e) {
            log.error("Airtime topup processing failed: id={}", topup.getId(), e);
            topup.setStatus(AirtimeTopup.TopupStatus.FAILED);
            topup.setTransactionRef(e.getMessage());
            airtimeTopupRepository.save(topup);
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR, "Airtime topup failed: " + e.getMessage());
        }

        return mapToResponse(topup);
    }

    @Override
    public Page<AirtimeTopupResponse> getHistory(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<AirtimeTopup> topups = airtimeTopupRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);
        return topups.map(this::mapToResponse);
    }

    @Override
    public List<AirtimeProvider> getProviders() {
        return Arrays.asList(AirtimeProvider.values());
    }

    private void processTopup(AirtimeTopup topup) {
        UUID txnId = topup.getId();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        WebClient webClient = webClientBuilder.build();

        Map<String, Object> debitRequest = Map.of(
                "walletId", topup.getUserId().toString(),
                "amount", topup.getAmount(),
                "description", "Airtime topup - " + topup.getProvider(),
                "txnId", txnId.toString()
        );

        webClient.post()
                .uri(WALLET_SERVICE_BASE + "/debit")
                .bodyValue(debitRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        topup.setStatus(AirtimeTopup.TopupStatus.COMPLETED);
        topup.setTransactionRef(txnId.toString());
        airtimeTopupRepository.save(topup);

        publishAirtimeCompletedEvent(topup);

        log.info("Airtime topup completed: id={}, phone={}, provider={}, amount={}",
                txnId, topup.getPhone(), topup.getProvider(), topup.getAmount());
    }

    private void publishAirtimeCompletedEvent(AirtimeTopup topup) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .userId(topup.getUserId())
                    .channel("SMS")
                    .type("AIRTIME_TOPUP_COMPLETED")
                    .title("Airtime Top-Up Successful")
                    .body(String.format("Your %s top-up of %d MMK to %s was successful.",
                            topup.getProvider(), topup.getAmount(), topup.getPhone()))
                    .phone(topup.getPhone())
                    .timestamp(OffsetDateTime.now())
                    .build();
            kafkaTemplate.send(AIRTIME_COMPLETED_TOPIC, topup.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish airtime completed event: id={}", topup.getId(), e);
        }
    }

    private void checkIdempotency(String idempotencyKey) {
        String cacheKey = AppConstants.IDEMPOTENCY_CACHE_PREFIX + idempotencyKey;
        Boolean exists = redisTemplate.hasKey(cacheKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BusinessException(ErrorCodes.DUPLICATE_TRANSACTION,
                    "Duplicate airtime topup detected for idempotency key: " + idempotencyKey);
        }
        redisTemplate.opsForValue().set(cacheKey, "1", 24, TimeUnit.HOURS);
    }

    private void validateProvider(AirtimeProvider provider) {
        if (!EnumUtils.isValidEnum(AirtimeProvider.class, provider.name())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Invalid airtime provider: " + provider);
        }
    }

    private AirtimeTopupResponse mapToResponse(AirtimeTopup topup) {
        return AirtimeTopupResponse.builder()
                .id(topup.getId())
                .phone(topup.getPhone())
                .amount(topup.getAmount())
                .provider(topup.getProvider())
                .status(topup.getStatus())
                .transactionRef(topup.getTransactionRef())
                .createdAt(topup.getCreatedAt())
                .build();
    }
}
