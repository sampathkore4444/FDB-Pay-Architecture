package com.fdbpay.services;

import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ReportingService {

    Map<String, Object> getDashboardMetrics();

    Map<String, Object> getTransactionSummary(String startDate, String endDate);

    Map<String, Object> getMerchantPerformanceReport(int page, int size);

    Map<String, Object> getComplianceReport(String month);

    Map<String, Object> getUserAcquisitionReport(String startDate, String endDate);

    byte[] exportReport(String reportType, String startDate, String endDate, String format);
}
