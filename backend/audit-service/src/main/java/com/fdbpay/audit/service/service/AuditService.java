package com.fdbpay.audit.service.service;

import com.fdbpay.audit.service.dto.request.AuditLogRequest;
import com.fdbpay.audit.service.dto.response.AuditEntryResponse;
import com.fdbpay.audit.service.dto.response.AuditSummaryResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface AuditService {

    AuditEntryResponse logAction(AuditLogRequest request);

    Page<AuditEntryResponse> getAllAuditLogs(int page, int size);

    Page<AuditEntryResponse> getAuditLog(String actorId, int page, int size);

    Page<AuditEntryResponse> getResourceAuditLog(String resourceType, String resourceId, int page, int size);

    Page<AuditEntryResponse> getActionAuditLog(String action, int page, int size);

    AuditSummaryResponse getSummary(LocalDate startDate, LocalDate endDate);

    String exportAuditLog(LocalDate startDate, LocalDate endDate, String format);
}
