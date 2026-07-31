package com.fdbpay.settlement.service.consumer;

import com.fdbpay.shared.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String DAILY_HASH_PREFIX = "merchant:daily:";
    private static final String MERCHANT_SERVICE_BASE = "http://merchant-service/merchant";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @KafkaListener(topics = "txn.completed", groupId = "settlement-service")
    public void handleTransactionCompleted(TransactionEvent event) {
        try {
            if (!"COMPLETED".equals(event.getStatus())) {
                return;
            }

            UUID receiverUserId = event.getReceiverUserId();
            UUID merchantCandidate = receiverUserId != null ? receiverUserId : event.getSenderUserId();
            if (merchantCandidate == null) {
                return;
            }

            String merchantId = resolveMerchantId(merchantCandidate);
            if (merchantId == null) {
                log.debug("No merchant found for transaction {}", event.getTransactionId());
                return;
            }

            String date = event.getTimestamp() != null
                    ? event.getTimestamp().toLocalDate().format(DATE_FORMAT)
                    : LocalDate.now().format(DATE_FORMAT);

            String hashKey = DAILY_HASH_PREFIX + date + ":" + merchantId;

            HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
            hashOps.increment(hashKey, "grossAmount", event.getAmount());
            hashOps.increment(hashKey, "transactionCount", 1);

            redisTemplate.expire(hashKey, 7, java.util.concurrent.TimeUnit.DAYS);

            log.debug("Aggregated transaction for settlement: merchantId={}, date={}, amount={}",
                    merchantId, date, event.getAmount());

        } catch (Exception e) {
            log.error("Failed to process transaction event for settlement: transactionId={}",
                    event.getTransactionId(), e);
        }
    }

    private String resolveMerchantId(UUID userId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    MERCHANT_SERVICE_BASE + "/by-user/" + userId, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }
            Object data = response.getBody().get("data");
            Object id = data != null ? ((Map<?, ?>) data).get("id") : null;
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            log.debug("Merchant lookup failed for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }
}
