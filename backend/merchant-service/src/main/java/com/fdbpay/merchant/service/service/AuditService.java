package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.response.AuditLogResponse;
import com.fdbpay.merchant.service.model.MerchantAuditLog;
import com.fdbpay.merchant.service.repository.MerchantAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final MerchantAuditLogRepository auditLogRepository;

    @Transactional
    public void log(UUID merchantId, String actorType, String actorName, UUID staffId,
                    String action, String entity, String entityId, String details) {
        try {
            auditLogRepository.save(MerchantAuditLog.builder()
                    .merchantId(merchantId)
                    .actorType(actorType)
                    .actorName(actorName)
                    .staffId(staffId)
                    .action(action)
                    .entity(entity)
                    .entityId(entityId)
                    .details(details)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write audit log: merchantId={}, action={}: {}", merchantId, action, e.getMessage());
        }
    }

    public List<AuditLogResponse> list(UUID merchantId, UUID staffId) {
        List<MerchantAuditLog> logs = staffId != null
                ? auditLogRepository.findByMerchantIdAndStaffIdOrderByCreatedAtDesc(merchantId, staffId)
                : auditLogRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        return logs.stream().map(this::mapToResponse).toList();
    }

    private AuditLogResponse mapToResponse(MerchantAuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .actorType(log.getActorType())
                .actorName(log.getActorName())
                .staffId(log.getStaffId())
                .action(log.getAction())
                .entity(log.getEntity())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
