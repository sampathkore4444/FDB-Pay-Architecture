package com.fdbpay.transfer.service.service;

import com.fdbpay.transfer.service.dto.response.analytics.AnalyticsTransactionRow;
import com.fdbpay.transfer.service.dto.response.analytics.MerchantAnalyticsBenchmark;
import com.fdbpay.transfer.service.dto.response.analytics.MerchantAnalyticsSummary;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface MerchantAnalyticsService {

    MerchantAnalyticsSummary getSummary(UUID walletId, LocalDate startDate, LocalDate endDate);

    MerchantAnalyticsBenchmark getBenchmark(UUID walletId, LocalDate startDate, LocalDate endDate);

    Page<AnalyticsTransactionRow> getTransactions(
            UUID walletId, LocalDate startDate, LocalDate endDate,
            String direction, Long minAmount, Long maxAmount, String method,
            String terminalId, String staffId, int page, int size);
}
