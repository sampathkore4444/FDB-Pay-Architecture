package com.fdbpay.audit.service.controller;

import com.fdbpay.audit.service.dto.request.AuditLogRequest;
import com.fdbpay.audit.service.dto.response.AuditEntryResponse;
import com.fdbpay.audit.service.dto.response.AuditSummaryResponse;
import com.fdbpay.audit.service.service.AuditService;
import com.fdbpay.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    public ApiResponse<Page<AuditEntryResponse>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(auditService.getAllAuditLogs(page, size));
    }

    @GetMapping("/log")
    public ApiResponse<Page<AuditEntryResponse>> searchLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String type = resourceType != null ? resourceType : resource;
        return ApiResponse.success(auditService.searchAuditLog(actor, action, type, resourceId, startDate, endDate, page, size));
    }

    @PostMapping("/log")
    public ApiResponse<AuditEntryResponse> logAction(@RequestBody AuditLogRequest request) {
        return ApiResponse.success(auditService.logAction(request));
    }

    @GetMapping("/actor/{actorId}")
    public ApiResponse<Page<AuditEntryResponse>> getAuditLogByActor(
            @PathVariable String actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(auditService.getAuditLog(actorId, page, size));
    }

    @GetMapping("/resource/{type}/{id}")
    public ApiResponse<Page<AuditEntryResponse>> getAuditLogByResource(
            @PathVariable String type,
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(auditService.getResourceAuditLog(type, id, page, size));
    }

    @GetMapping("/resource")
    public ApiResponse<Page<AuditEntryResponse>> getAuditLogByResourceQuery(
            @RequestParam String resourceType,
            @RequestParam String resourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(auditService.getResourceAuditLog(resourceType, resourceId, page, size));
    }

    @GetMapping("/action/{action}")
    public ApiResponse<Page<AuditEntryResponse>> getAuditLogByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(auditService.getActionAuditLog(action, page, size));
    }

    @GetMapping("/summary")
    public ApiResponse<AuditSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate from = startDate != null ? startDate : LocalDate.now().minusDays(90);
        LocalDate to = endDate != null ? endDate : LocalDate.now();
        return ApiResponse.success(auditService.getSummary(from, to));
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportAuditLog(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "csv") String format) {

        LocalDate from = startDate != null ? startDate : LocalDate.now().minusDays(90);
        LocalDate to = endDate != null ? endDate : LocalDate.now();
        String content = auditService.exportAuditLog(from, to, format);

        String contentType = "csv".equalsIgnoreCase(format) ? "text/csv" : "application/json";
        String filename = "audit-log-" + from + "-to-" + to + "." + format.toLowerCase();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }
}
