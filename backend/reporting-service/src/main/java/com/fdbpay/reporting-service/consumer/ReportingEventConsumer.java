package com.fdbpay.reporting.service.consumer;

import com.fdbpay.shared.event.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ReportingEventConsumer {

    @KafkaListener(
            topics = "txn.completed",
            groupId = "reporting-service"
    )
    public void onTransactionCompleted(ConsumerRecord<String, TransactionEvent> record) {
        TransactionEvent event = record.value();
        log.debug("Recording completed transaction {} for reporting read model", event.getTransactionId());

        try {
            updateTransactionMetrics(event);
        } catch (Exception e) {
            log.error("Error updating transaction metrics for {}: {}", event.getTransactionId(), e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "merchant.settlement",
            groupId = "reporting-service"
    )
    public void onMerchantSettlement(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> event = record.value();
        log.debug("Recording merchant settlement event: {}", event);

        try {
            updateSettlementMetrics(event);
        } catch (Exception e) {
            log.error("Error updating settlement metrics: {}", e.getMessage(), e);
        }
    }

    private void updateTransactionMetrics(TransactionEvent event) {
        log.info("Updating local read model - transaction: {}, type: {}, amount: {}, status: {}",
                event.getTransactionId(), event.getType(), event.getAmount(), event.getStatus());
    }

    private void updateSettlementMetrics(Map<String, Object> event) {
        log.info("Updating local read model - merchant settlement: {}", event);
    }
}
