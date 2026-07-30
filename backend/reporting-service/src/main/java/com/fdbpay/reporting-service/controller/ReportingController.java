package com.fdbpay.reporting.service.controller;

import com.fdbpay.reporting.service.dto.response.ComplianceReport;
import com.fdbpay.reporting.service.dto.response.DashboardMetrics;
import com.fdbpay.reporting.service.dto.response.TransactionSummary;
import com.fdbpay.reporting.service.service.ReportingService;
import com.fdbpay.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Reporting", description = "Admin reporting and analytics endpoints")
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/reports")
    @Operation(summary = "Get paginated reports")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        DashboardMetrics metrics = reportingService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "metrics", metrics,
                "page", page,
                "size", size
        )));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard metrics")
    public ResponseEntity<ApiResponse<DashboardMetrics>> getDashboardMetrics() {
        DashboardMetrics metrics = reportingService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/transactions/summary")
    @Operation(summary = "Get transaction summary for a date range")
    public ResponseEntity<ApiResponse<TransactionSummary>> getTransactionSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        TransactionSummary summary = reportingService.getTransactionSummary(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/merchants/performance")
    @Operation(summary = "Get merchant performance report")
    public ResponseEntity<ApiResponse<Object>> getMerchantPerformance(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Object report = reportingService.getMerchantPerformanceReport(page, size);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/compliance/{month}")
    @Operation(summary = "Get compliance report for a given month")
    public ResponseEntity<ApiResponse<ComplianceReport>> getComplianceReport(@PathVariable String month) {
        ComplianceReport report = reportingService.getComplianceReport(month);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/export")
    @Operation(summary = "Export a report in specified format")
    public ResponseEntity<ApiResponse<Object>> exportReport(
            @RequestParam String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format) {
        Object export = reportingService.exportReport(type, startDate, endDate, format);
        return ResponseEntity.ok(ApiResponse.success(export));
    }
}
