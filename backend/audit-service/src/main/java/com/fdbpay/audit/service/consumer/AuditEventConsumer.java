package com.fdbpay.audit.service.consumer;

import com.fdbpay.audit.service.dto.request.AuditLogRequest;
import com.fdbpay.audit.service.service.AuditService;
import com.fdbpay.shared.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditService auditService;

    @KafkaListener(topics = "audit.log", groupId = "audit-service")
    public void handleAuditLog(Map<String, Object> event) {
        try {
            AuditLogRequest request = AuditLogRequest.builder()
                    .actorId(event.containsKey("actorId") ? UUID.fromString(event.get("actorId").toString()) : null)
                    .actorType((String) event.getOrDefault("actorType", "SYSTEM"))
                    .actorName((String) event.get("actorName"))
                    .action((String) event.get("action"))
                    .resourceType((String) event.get("resourceType"))
                    .resourceId((String) event.get("resourceId"))
                    .oldValues(event.containsKey("oldValues") ? event.get("oldValues").toString() : null)
                    .newValues(event.containsKey("newValues") ? event.get("newValues").toString() : null)
                    .ipAddress((String) event.get("ipAddress"))
                    .userAgent((String) event.get("userAgent"))
                    .sessionId((String) event.get("sessionId"))
                    .build();

            auditService.logAction(request);
        } catch (Exception e) {
            log.error("Failed to process audit.log event", e);
        }
    }

    @KafkaListener(topics = "txn.completed", groupId = "audit-service")
    public void handleTransactionCompleted(TransactionEvent event) {
        try {
            AuditLogRequest request = AuditLogRequest.builder()
                    .actorId(event.getSenderUserId())
                    .actorType("USER")
                    .action("TRANSFER_COMPLETED")
                    .resourceType("TRANSACTION")
                    .resourceId(event.getTransactionId().toString())
                    .newValues("{\"status\":\"" + event.getStatus() + "\",\"amount\":" + event.getAmount() + "}")
                    .build();
            auditService.logAction(request);
        } catch (Exception e) {
            log.error("Failed to create audit entry for txn.completed", e);
        }
    }

    @KafkaListener(topics = "kyc.reviewed", groupId = "audit-service")
    public void handleKycReviewed(Map<String, Object> event) {
        try {
            AuditLogRequest request = AuditLogRequest.builder()
                    .actorId(event.containsKey("reviewerId") ? UUID.fromString(event.get("reviewerId").toString()) : null)
                    .actorType("ADMIN")
                    .action("KYC_REVIEWED")
                    .resourceType("USER")
                    .resourceId((String) event.getOrDefault("userId", ""))
                    .newValues("{\"status\":\"" + event.getOrDefault("status", "") + "\"}")
                    .build();
            auditService.logAction(request);
        } catch (Exception e) {
            log.error("Failed to create audit entry for kyc.reviewed", e);
        }
    }

    @KafkaListener(topics = "settlement.completed", groupId = "audit-service")
    public void handleSettlementCompleted(Map<String, Object> event) {
        try {
            AuditLogRequest request = AuditLogRequest.builder()
                    .actorType("SYSTEM")
                    .action("SETTLEMENT_COMPLETED")
                    .resourceType("SETTLEMENT")
                    .resourceId((String) event.getOrDefault("settlementId", ""))
                    .newValues("{\"merchantId\":\"" + event.getOrDefault("merchantId", "")
                            + "\",\"netAmount\":" + event.getOrDefault("netAmount", 0) + "}")
                    .build();
            auditService.logAction(request);
        } catch (Exception e) {
            log.error("Failed to create audit entry for settlement.completed", e);
        }
    }

    @KafkaListener(topics = "dispute.resolved", groupId = "audit-service")
    public void handleDisputeResolved(Map<String, Object> event) {
        try {
            AuditLogRequest request = AuditLogRequest.builder()
                    .actorId(event.containsKey("resolvedBy") ? UUID.fromString(event.get("resolvedBy").toString()) : null)
                    .actorType("ADMIN")
                    .action("DISPUTE_RESOLVED")
                    .resourceType("DISPUTE")
                    .resourceId((String) event.getOrDefault("disputeId", ""))
                    .newValues("{\"status\":\"" + event.getOrDefault("status", "") + "\"}")
                    .build();
            auditService.logAction(request);
        } catch (Exception e) {
            log.error("Failed to create audit entry for dispute.resolved", e);
        }
    }
}
