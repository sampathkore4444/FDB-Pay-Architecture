package com.fdbpay.reporting.service.service;

import com.fdbpay.reporting.service.dto.response.ComplianceReport;
import com.fdbpay.reporting.service.dto.response.DashboardMetrics;
import com.fdbpay.reporting.service.dto.response.TransactionSummary;

import java.time.LocalDate;

public interface ReportingService {

    DashboardMetrics getDashboardMetrics();

    TransactionSummary getTransactionSummary(LocalDate startDate, LocalDate endDate);

    Object getMerchantPerformanceReport(int page, int size);

    ComplianceReport getComplianceReport(String month);

    Object exportReport(String type, LocalDate startDate, LocalDate endDate, String format);
}
