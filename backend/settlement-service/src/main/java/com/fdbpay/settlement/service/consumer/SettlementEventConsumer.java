package com.fdbpay.settlement.service.consumer;

import com.fdbpay.shared.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DAILY_HASH_PREFIX = "merchant:daily:";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @KafkaListener(topics = "txn.completed", groupId = "settlement-service")
    public void handleTransactionCompleted(TransactionEvent event) {
        try {
            if (!"COMPLETED".equals(event.getStatus())) {
                return;
            }

            String date = event.getTimestamp() != null
                    ? event.getTimestamp().toLocalDate().format(DATE_FORMAT)
                    : LocalDate.now().format(DATE_FORMAT);

            String merchantId = event.getReceiverUserId() != null
                    ? event.getReceiverUserId().toString()
                    : event.getSenderUserId().toString();

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
}
