package com.fdbpay.events;

import com.fdbpay.models.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTransactionCompleted(Transaction transaction) {
        String topic = "txn.completed";
        log.info("Publishing transaction completed event: txnId={}, topic={}", transaction.getId(), topic);
        kafkaTemplate.send(topic, transaction.getId().toString(), transaction);
    }

    public void publishMerchantEvent(String eventType, Object payload) {
        String topic = "merchant." + eventType;
        log.info("Publishing merchant event: type={}, topic={}", eventType, topic);
        kafkaTemplate.send(topic, payload);
    }

    public void publishKycEvent(String eventType, Object payload) {
        String topic = "kyc." + eventType;
        log.info("Publishing KYC event: type={}, topic={}", eventType, topic);
        kafkaTemplate.send(topic, payload);
    }

    public void publishSettlementEvent(String eventType, Object payload) {
        String topic = "settlement." + eventType;
        log.info("Publishing settlement event: type={}, topic={}", eventType, topic);
        kafkaTemplate.send(topic, payload);
    }

    public void publishAuditEvent(Object payload) {
        String topic = "audit.log";
        kafkaTemplate.send(topic, payload);
    }
}
