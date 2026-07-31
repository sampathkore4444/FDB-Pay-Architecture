package com.fdbpay.audit.service.service.impl;

import com.fdbpay.audit.service.dto.request.AuditLogRequest;
import com.fdbpay.audit.service.dto.response.AuditEntryResponse;
import com.fdbpay.audit.service.dto.response.AuditSummaryResponse;
import com.fdbpay.audit.service.model.AuditEntry;
import com.fdbpay.audit.service.repository.AuditEntryRepository;
import com.fdbpay.audit.service.service.AuditService;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditEntryRepository auditEntryRepository;

    @Override
    @Transactional
    public AuditEntryResponse logAction(AuditLogRequest request) {
        AuditEntry entry = AuditEntry.builder()
                .actorId(request.getActorId())
                .actorType(request.getActorType())
                .actorName(request.getActorName())
                .action(request.getAction())
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .oldValues(request.getOldValues())
                .newValues(request.getNewValues())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .sessionId(request.getSessionId())
                .createdAt(OffsetDateTime.now())
                .build();

        entry = auditEntryRepository.save(entry);
        log.debug("Audit entry created: id={}, action={}, actorType={}, resource={}/{}",
                entry.getId(), entry.getAction(), entry.getActorType(),
                entry.getResourceType(), entry.getResourceId());

        return mapToResponse(entry);
    }

    @Override
    public Page<AuditEntryResponse> getAllAuditLogs(int page, int size) {
        Page<AuditEntry> entries = auditEntryRepository.findAll(PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        return entries.map(this::mapToResponse);
    }

    @Override
    public Page<AuditEntryResponse> getAuditLog(String actorId, int page, int size) {
        UUID actorUuid = UUID.fromString(actorId);
        Page<AuditEntry> entries = auditEntryRepository
                .findByActorIdOrderByCreatedAtDesc(actorUuid, PageRequest.of(page, size));
        return entries.map(this::mapToResponse);
    }

    @Override
    public Page<AuditEntryResponse> getResourceAuditLog(String resourceType, String resourceId, int page, int size) {
        Page<AuditEntry> entries = auditEntryRepository
                .findByResourceTypeAndResourceIdOrderByCreatedAtDesc(resourceType, resourceId, PageRequest.of(page, size));
        return entries.map(this::mapToResponse);
    }

    @Override
    public Page<AuditEntryResponse> getActionAuditLog(String action, int page, int size) {
        Page<AuditEntry> entries = auditEntryRepository
                .findByActionOrderByCreatedAtDesc(action, PageRequest.of(page, size));
        return entries.map(this::mapToResponse);
    }

    @Override
    public Page<AuditEntryResponse> searchAuditLog(String actorId, String action, String resourceType, String resourceId,
                                                   LocalDate startDate, LocalDate endDate, int page, int size) {
        if (actorId != null && !actorId.isBlank()) {
            try {
                return getAuditLog(actorId, page, size);
            } catch (IllegalArgumentException e) {
                return Page.empty();
            }
        }
        if (action != null && !action.isBlank()) {
            return getActionAuditLog(action, page, size);
        }
        if (resourceType != null && !resourceType.isBlank()) {
            if (resourceId != null && !resourceId.isBlank()) {
                return getResourceAuditLog(resourceType, resourceId, page, size);
            }
            Page<AuditEntry> entries = auditEntryRepository
                    .findByResourceTypeOrderByCreatedAtDesc(resourceType, PageRequest.of(page, size));
            return entries.map(this::mapToResponse);
        }
        if (startDate != null && endDate != null) {
            OffsetDateTime start = startDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            OffsetDateTime end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            Page<AuditEntry> entries = auditEntryRepository
                    .findByCreatedAtBetweenOrderByCreatedAtDesc(start, end, PageRequest.of(page, size));
            return entries.map(this::mapToResponse);
        }
        return getAllAuditLogs(page, size);
    }

    @Override
    public AuditSummaryResponse getSummary(LocalDate startDate, LocalDate endDate) {
        OffsetDateTime start = startDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        long totalActions = auditEntryRepository.countByCreatedAtBetween(start, end);
        long uniqueActors = auditEntryRepository.countDistinctActorIdByCreatedAtBetween(start, end);

        Map<String, Long> byAction = new LinkedHashMap<>();
        List<Object[]> actionCounts = auditEntryRepository.countByActionBetween(start, end);
        for (Object[] row : actionCounts) {
            byAction.put((String) row[0], (Long) row[1]);
        }

        Map<String, Long> byActorType = new LinkedHashMap<>();
        List<Object[]> actorTypeCounts = auditEntryRepository.countByActorTypeBetween(start, end);
        for (Object[] row : actorTypeCounts) {
            byActorType.put((String) row[0], (Long) row[1]);
        }

        List<AuditSummaryResponse.TopAction> topActions = byAction.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> AuditSummaryResponse.TopAction.builder().action(e.getKey()).count(e.getValue()).build())
                .toList();

        return AuditSummaryResponse.builder()
                .totalActions(totalActions)
                .totalEvents(totalActions)
                .uniqueActors(uniqueActors)
                .topActions(topActions)
                .byAction(byAction)
                .byActorType(byActorType)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    @Override
    public String exportAuditLog(LocalDate startDate, LocalDate endDate, String format) {
        OffsetDateTime start = startDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        List<AuditEntry> entries = auditEntryRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);

        if ("json".equalsIgnoreCase(format)) {
            return exportAsJson(entries);
        }
        return exportAsCsv(entries);
    }

    private String exportAsCsv(List<AuditEntry> entries) {
        StringWriter writer = new StringWriter();
        writer.write("id,actorId,actorType,actorName,action,resourceType,resourceId,ipAddress,userAgent,createdAt\n");
        for (AuditEntry entry : entries) {
            writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    entry.getId(),
                    entry.getActorId(),
                    entry.getActorType(),
                    safeString(entry.getActorName()),
                    entry.getAction(),
                    entry.getResourceType(),
                    safeString(entry.getResourceId()),
                    safeString(entry.getIpAddress()),
                    safeString(entry.getUserAgent()),
                    entry.getCreatedAt()));
        }
        return writer.toString();
    }

    private String exportAsJson(List<AuditEntry> entries) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < entries.size(); i++) {
            AuditEntry entry = entries.get(i);
            sb.append("  {")
                    .append("\"id\":\"").append(entry.getId()).append("\",")
                    .append("\"actorId\":\"").append(entry.getActorId()).append("\",")
                    .append("\"actorType\":\"").append(entry.getActorType()).append("\",")
                    .append("\"actorName\":\"").append(safeString(entry.getActorName())).append("\",")
                    .append("\"action\":\"").append(entry.getAction()).append("\",")
                    .append("\"resourceType\":\"").append(entry.getResourceType()).append("\",")
                    .append("\"resourceId\":\"").append(safeString(entry.getResourceId())).append("\",")
                    .append("\"oldValues\":").append(entry.getOldValues() != null ? entry.getOldValues() : "null").append(",")
                    .append("\"newValues\":").append(entry.getNewValues() != null ? entry.getNewValues() : "null").append(",")
                    .append("\"ipAddress\":\"").append(safeString(entry.getIpAddress())).append("\",")
                    .append("\"userAgent\":\"").append(safeString(entry.getUserAgent())).append("\",")
                    .append("\"sessionId\":\"").append(safeString(entry.getSessionId())).append("\",")
                    .append("\"createdAt\":\"").append(entry.getCreatedAt()).append("\"")
                    .append("}");
            if (i < entries.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private String safeString(String value) {
        return value != null ? value.replace("\"", "\\\"") : "";
    }

    private AuditEntryResponse mapToResponse(AuditEntry entry) {
        return AuditEntryResponse.builder()
                .id(entry.getId())
                .actorId(entry.getActorId())
                .actorType(entry.getActorType())
                .actorName(entry.getActorName())
                .action(entry.getAction())
                .resourceType(entry.getResourceType())
                .resourceId(entry.getResourceId())
                .oldValues(entry.getOldValues())
                .newValues(entry.getNewValues())
                .ipAddress(entry.getIpAddress())
                .userAgent(entry.getUserAgent())
                .sessionId(entry.getSessionId())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
