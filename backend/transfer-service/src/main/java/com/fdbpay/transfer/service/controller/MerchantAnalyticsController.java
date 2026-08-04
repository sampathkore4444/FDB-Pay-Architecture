package com.fdbpay.transfer.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.transfer.service.dto.response.analytics.AnalyticsTransactionRow;
import com.fdbpay.transfer.service.dto.response.analytics.MerchantAnalyticsBenchmark;
import com.fdbpay.transfer.service.dto.response.analytics.MerchantAnalyticsSummary;
import com.fdbpay.transfer.service.service.MerchantAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/transfer/analytics")
@RequiredArgsConstructor
public class MerchantAnalyticsController {

    private final MerchantAnalyticsService merchantAnalyticsService;

    @GetMapping("/summary")
    public ApiResponse<MerchantAnalyticsSummary> getSummary(
            @RequestParam UUID walletId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(merchantAnalyticsService.getSummary(walletId, startDate, endDate));
    }

    @GetMapping("/benchmark")
    public ApiResponse<MerchantAnalyticsBenchmark> getBenchmark(
            @RequestParam UUID walletId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(merchantAnalyticsService.getBenchmark(walletId, startDate, endDate));
    }

    @GetMapping("/transactions")
    public ApiResponse<Page<AnalyticsTransactionRow>> getTransactions(
            @RequestParam UUID walletId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) Long minAmount,
            @RequestParam(required = false) Long maxAmount,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String terminalId,
            @RequestParam(required = false) String staffId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(merchantAnalyticsService.getTransactions(
                walletId, startDate, endDate, direction, minAmount, maxAmount, method, terminalId, staffId, page, size));
    }
}
