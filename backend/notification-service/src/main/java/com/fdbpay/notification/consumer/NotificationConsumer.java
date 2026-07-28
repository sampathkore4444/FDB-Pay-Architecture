package com.fdbpay.notification.consumer;

import com.fdbpay.notification.service.NotificationService;
import com.fdbpay.shared.event.NotificationEvent;
import com.fdbpay.shared.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "txn.completed", groupId = "notification-service")
    public void handleTransactionCompleted(ConsumerRecord<String, TransactionEvent> record) {
        TransactionEvent event = record.value();
        log.info("Received transaction.completed event: txnId={}, type={}, status={}",
                event.getTransactionId(), event.getType(), event.getStatus());

        if ("COMPLETED".equals(event.getStatus())) {
            notificationService.sendTransactionNotification(
                    event.getReceiverUserId(),
                    event.getType(),
                    event.getAmount(),
                    event.getTransactionId().toString()
            );
        }
    }

    @KafkaListener(topics = "settlement.daily", groupId = "notification-service")
    public void handleSettlementDaily(ConsumerRecord<String, TransactionEvent> record) {
        TransactionEvent event = record.value();
        log.info("Received settlement.daily event: txnId={}", event.getTransactionId());

        notificationService.sendSettlementNotification(
                event.getReceiverUserId(),
                event.getAmount(),
                event.getTransactionId().toString()
        );
    }

    @KafkaListener(topics = "notification.send", groupId = "notification-service")
    public void handleNotificationSend(ConsumerRecord<String, NotificationEvent> record) {
        NotificationEvent event = record.value();
        log.info("Received notification.send event: userId={}, channel={}, type={}",
                event.getUserId(), event.getChannel(), event.getType());

        switch (event.getType()) {
            case "OTP" -> notificationService.sendOtpSms(event.getPhone(), event.getBody());
            case "TRANSACTION" -> notificationService.sendTransactionNotification(
                    event.getUserId(), event.getTitle(), 0L, event.getBody());
            case "PUSH" -> notificationService.sendPushNotification(
                    event.getUserId(), event.getTitle(), event.getBody());
            case "SETTLEMENT" -> notificationService.sendSettlementNotification(
                    event.getUserId(), 0L, event.getBody());
            default -> log.warn("Unknown notification type: {}", event.getType());
        }
    }
}
