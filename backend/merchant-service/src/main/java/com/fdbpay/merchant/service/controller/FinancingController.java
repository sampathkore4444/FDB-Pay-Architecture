package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.FinancingApplicationRequest;
import com.fdbpay.merchant.service.dto.response.FinancingApplicationResponse;
import com.fdbpay.merchant.service.dto.response.FinancingEligibilityResponse;
import com.fdbpay.merchant.service.service.FinancingService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/financing")
@RequiredArgsConstructor
public class FinancingController {

    private final FinancingService financingService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping("/eligibility")
    public ApiResponse<FinancingEligibilityResponse> eligibility(
            @RequestParam UUID userId,
            @RequestParam UUID walletId) {
        return ApiResponse.success(financingService.getEligibility(accessHelper.resolveMerchantId(userId), walletId));
    }

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FinancingApplicationResponse> apply(
            @RequestParam UUID userId,
            @RequestParam UUID walletId,
            @Valid @RequestBody FinancingApplicationRequest request) {
        return ApiResponse.success(financingService.apply(accessHelper.resolveMerchantId(userId), walletId, request));
    }

    @GetMapping("/applications")
    public ApiResponse<List<FinancingApplicationResponse>> getApplications(@RequestParam UUID userId) {
        return ApiResponse.success(financingService.getApplications(accessHelper.resolveMerchantId(userId)));
    }

    @GetMapping("/applications/{applicationId}")
    public ApiResponse<FinancingApplicationResponse> getApplication(
            @RequestParam UUID userId,
            @PathVariable UUID applicationId) {
        return ApiResponse.success(financingService.getApplication(accessHelper.resolveMerchantId(userId), applicationId));
    }
}
