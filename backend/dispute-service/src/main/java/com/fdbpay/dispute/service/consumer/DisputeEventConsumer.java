package com.fdbpay.dispute.service.consumer;

import com.fdbpay.shared.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisputeEventConsumer {

    @KafkaListener(topics = "txn.completed", groupId = "dispute-service")
    public void handleTransactionCompleted(TransactionEvent event) {
        log.debug("Received transaction completed event: transactionId={}, status={}",
                event.getTransactionId(), event.getStatus());
    }

    @KafkaListener(topics = "dispute.created", groupId = "dispute-service")
    public void handleDisputeCreated(Map<String, Object> event) {
        try {
            String disputeId = event.get("disputeId").toString();
            String complainantUserId = event.get("complainantUserId").toString();
            String type = event.get("type").toString();
            log.info("New dispute created: disputeId={}, complainantUserId={}, type={}", disputeId, complainantUserId, type);
        } catch (Exception e) {
            log.error("Failed to process dispute.created event", e);
        }
    }
}
