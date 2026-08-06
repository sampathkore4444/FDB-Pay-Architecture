package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.RecurringPlanRequest;
import com.fdbpay.merchant.service.dto.response.RecurringPlanResponse;
import com.fdbpay.merchant.service.model.enums.RecurringPlanStatus;
import com.fdbpay.merchant.service.service.RecurringBillingService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/recurring-plans")
@RequiredArgsConstructor
public class RecurringBillingController {

    private final RecurringBillingService recurringBillingService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping
    public ApiResponse<List<RecurringPlanResponse>> list(@RequestParam UUID userId) {
        return ApiResponse.success(recurringBillingService.list(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecurringPlanResponse> create(@RequestParam UUID userId, @Valid @RequestBody RecurringPlanRequest request) {
        return ApiResponse.success(recurringBillingService.create(accessHelper.resolveMerchantId(userId), request));
    }

    @PutMapping("/{planId}")
    public ApiResponse<RecurringPlanResponse> update(@RequestParam UUID userId, @PathVariable UUID planId,
                                                     @Valid @RequestBody RecurringPlanRequest request) {
        return ApiResponse.success(recurringBillingService.update(accessHelper.resolveMerchantId(userId), planId, request));
    }

    @PutMapping("/{planId}/status")
    public ApiResponse<RecurringPlanResponse> setStatus(@RequestParam UUID userId, @PathVariable UUID planId,
                                                        @RequestParam RecurringPlanStatus status) {
        return ApiResponse.success(recurringBillingService.setStatus(accessHelper.resolveMerchantId(userId), planId, status));
    }

    @PostMapping("/{planId}/run")
    public ApiResponse<RecurringPlanResponse> runNow(@RequestParam UUID userId, @PathVariable UUID planId) {
        return ApiResponse.success(recurringBillingService.runNow(accessHelper.resolveMerchantId(userId), planId));
    }

    @DeleteMapping("/{planId}")
    public ApiResponse<Void> delete(@RequestParam UUID userId, @PathVariable UUID planId) {
        recurringBillingService.delete(accessHelper.resolveMerchantId(userId), planId);
        return ApiResponse.success(null);
    }
}
