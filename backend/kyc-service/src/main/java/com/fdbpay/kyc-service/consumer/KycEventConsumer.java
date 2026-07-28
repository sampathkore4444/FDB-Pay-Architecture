package com.fdbpay.kyc.service.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KycEventConsumer {

    @KafkaListener(
            topics = {"kyc.submitted", "kyc.reviewed"},
            groupId = "kyc-service"
    )
    public void onKycEvent(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> event = record.value();
        log.info("Received KYC event on topic {}: {}", record.topic(), event);

        try {
            String eventType = record.topic();
            String userId = (String) event.get("userId");
            String status = (String) event.get("status");
            String tier = (String) event.get("tier");

            switch (eventType) {
                case "kyc.submitted" -> log.info("KYC submitted event processed for user {} with tier {}", userId, tier);
                case "kyc.reviewed" -> log.info("KYC reviewed event processed for user {} with status {}", userId, status);
                default -> log.warn("Unknown KYC event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing KYC event: {}", e.getMessage(), e);
        }
    }
}
