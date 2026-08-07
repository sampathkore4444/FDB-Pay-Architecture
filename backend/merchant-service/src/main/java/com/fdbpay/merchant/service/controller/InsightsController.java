package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.response.BestSellerResponse;
import com.fdbpay.merchant.service.dto.response.CashFlowForecastResponse;
import com.fdbpay.merchant.service.dto.response.FeeCalculationResponse;
import com.fdbpay.merchant.service.dto.response.MonitoringResponse;
import com.fdbpay.merchant.service.dto.response.RepeatCustomerResponse;
import com.fdbpay.merchant.service.service.CashFlowService;
import com.fdbpay.merchant.service.service.CustomerInsightService;
import com.fdbpay.merchant.service.service.FeeCalculatorService;
import com.fdbpay.merchant.service.service.MonitoringService;
import com.fdbpay.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/insights")
@RequiredArgsConstructor
public class InsightsController {

    private final FeeCalculatorService feeCalculatorService;
    private final CashFlowService cashFlowService;
    private final MonitoringService monitoringService;
    private final CustomerInsightService customerInsightService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping("/fee-calculator")
    public ApiResponse<FeeCalculationResponse> feeCalculator(@RequestParam UUID userId, @RequestParam Long amount) {
        return ApiResponse.success(feeCalculatorService.calculate(accessHelper.resolveMerchantId(userId), amount));
    }

    @GetMapping("/cash-flow")
    public ApiResponse<CashFlowForecastResponse> cashFlow(@RequestParam UUID userId,
                                                          @RequestParam(defaultValue = "12") int months) {
        return ApiResponse.success(cashFlowService.forecast(accessHelper.resolveMerchantId(userId), months));
    }

    @GetMapping("/monitoring")
    public ApiResponse<MonitoringResponse> monitoring(@RequestParam UUID userId) {
        return ApiResponse.success(monitoringService.monitor(accessHelper.resolveMerchantId(userId)));
    }

    @GetMapping("/best-sellers")
    public ApiResponse<List<BestSellerResponse>> bestSellers(@RequestParam UUID userId,
                                                             @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(customerInsightService.bestSellers(accessHelper.resolveMerchantId(userId), limit));
    }

    @GetMapping("/repeat-customers")
    public ApiResponse<List<RepeatCustomerResponse>> repeatCustomers(@RequestParam UUID userId,
                                                                     @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(customerInsightService.repeatCustomers(accessHelper.resolveMerchantId(userId), limit));
    }
}
