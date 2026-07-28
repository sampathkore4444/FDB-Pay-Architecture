package com.fdbpay.fraud.risk.service.consumer;

import com.fdbpay.fraud.risk.service.dto.request.TransactionEvaluationRequest;
import com.fdbpay.fraud.risk.service.dto.response.FraudEvaluationResponse;
import com.fdbpay.fraud.risk.service.service.FraudRiskService;
import com.fdbpay.shared.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudEventConsumer {

    private final FraudRiskService fraudRiskService;

    @KafkaListener(
            topics = "txn.completed",
            groupId = "fraud-risk-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionCompleted(ConsumerRecord<String, TransactionEvent> record) {
        TransactionEvent event = record.value();
        log.info("Received txn.completed event for transaction: {}", event.getTransactionId());

        try {
            TransactionEvaluationRequest request = TransactionEvaluationRequest.builder()
                    .transactionId(event.getTransactionId())
                    .senderUserId(event.getSenderUserId())
                    .receiverUserId(event.getReceiverUserId())
                    .amount(BigDecimal.valueOf(event.getAmount()))
                    .type(event.getType())
                    .build();

            FraudEvaluationResponse response = fraudRiskService.evaluateTransaction(request);

            if (!response.isApproved()) {
                log.warn("Transaction {} flagged as high risk with score {}. Reasons: {}",
                        event.getTransactionId(), response.getRiskScore(), response.getReasons());
            } else {
                log.info("Transaction {} passed fraud check with score {}",
                        event.getTransactionId(), response.getRiskScore());
            }
        } catch (Exception e) {
            log.error("Error evaluating transaction {}: {}", event.getTransactionId(), e.getMessage(), e);
        }
    }
}
