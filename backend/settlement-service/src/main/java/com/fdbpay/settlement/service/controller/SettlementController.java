package com.fdbpay.settlement.service.controller;

import com.fdbpay.settlement.service.dto.request.TriggerSettlementRequest;
import com.fdbpay.settlement.service.dto.response.SettlementBatchResponse;
import com.fdbpay.settlement.service.dto.response.SettlementResponse;
import com.fdbpay.settlement.service.dto.response.SettlementSummaryResponse;
import com.fdbpay.settlement.service.service.SettlementService;
import com.fdbpay.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping("/trigger")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<SettlementResponse> triggerMerchantSettlement(
            @RequestBody TriggerSettlementRequest request) {
        return ApiResponse.success(settlementService.triggerMerchantSettlement(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<SettlementResponse> getSettlement(@PathVariable UUID id) {
        return ApiResponse.success(settlementService.getSettlement(id));
    }

    @GetMapping("/merchant/{merchantId}")
    public ApiResponse<Page<SettlementResponse>> getMerchantSettlements(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(settlementService.getMerchantSettlements(merchantId, page, size));
    }

    @GetMapping("/batch/{batchId}/summary")
    public ApiResponse<SettlementSummaryResponse> getSettlementSummary(@PathVariable UUID batchId) {
        return ApiResponse.success(settlementService.getSettlementSummary(batchId));
    }

    @PostMapping("/reconcile")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<SettlementBatchResponse> reconcile() {
        return ApiResponse.success(settlementService.reconcile());
    }
}
