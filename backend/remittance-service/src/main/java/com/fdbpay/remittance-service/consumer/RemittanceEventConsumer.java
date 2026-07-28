package com.fdbpay.remittance.service.consumer;

import com.fdbpay.shared.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemittanceEventConsumer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @KafkaListener(topics = "remittance.received", groupId = "remittance-service")
    public void handleRemittanceReceived(NotificationEvent event) {
        log.info("Processing remittance received notification: userId={}, type={}", event.getUserId(), event.getType());

        NotificationEvent notification = NotificationEvent.builder()
                .userId(event.getUserId())
                .channel("SMS")
                .type("REMITTANCE_RECEIVED")
                .title("Remittance Received")
                .body(event.getBody())
                .phone(event.getPhone())
                .email(event.getEmail())
                .timestamp(OffsetDateTime.now())
                .build();

        kafkaTemplate.send("notification.send", event.getUserId().toString(), notification);
        log.info("Notification sent for remittance: userId={}", event.getUserId());
    }
}
