package com.fdbpay.events;

import com.fdbpay.models.entity.Transaction;
import com.fdbpay.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "txn.completed", groupId = "fdb-pay")
    public void handleTransactionCompleted(ConsumerRecord<String, Transaction> record) {
        Transaction transaction = record.value();
        log.info("Processing transaction completed event: txnId={}", transaction.getId());

        if (transaction.getSenderWallet() != null && transaction.getSenderWallet().getUser() != null) {
            notificationService.sendTransactionNotification(
                    transaction.getSenderWallet().getUser().getId().toString(),
                    "SENT",
                    transaction.getAmount(),
                    transaction.getId().toString());
        }

        if (transaction.getReceiverWallet() != null && transaction.getReceiverWallet().getUser() != null) {
            notificationService.sendTransactionNotification(
                    transaction.getReceiverWallet().getUser().getId().toString(),
                    "RECEIVED",
                    transaction.getAmount(),
                    transaction.getId().toString());
        }
    }

    @KafkaListener(topics = "settlement.daily", groupId = "fdb-pay")
    public void handleSettlementEvent(ConsumerRecord<String, String> record) {
        log.info("Processing settlement event: {}", record.value());
    }

    @KafkaListener(topics = "kyc.submitted", groupId = "fdb-pay")
    public void handleKycSubmitted(ConsumerRecord<String, String> record) {
        log.info("Processing KYC submitted event: {}", record.value());
    }
}
