package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.MerchantPreferencesRequest;
import com.fdbpay.merchant.service.dto.request.PayoutAccountRequest;
import com.fdbpay.merchant.service.dto.response.MerchantPreferencesResponse;
import com.fdbpay.merchant.service.dto.response.PayoutAccountResponse;
import com.fdbpay.merchant.service.service.PayoutService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping("/payout-accounts")
    public ApiResponse<List<PayoutAccountResponse>> listAccounts(@RequestParam UUID userId) {
        return ApiResponse.success(payoutService.listAccounts(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping("/payout-accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PayoutAccountResponse> createAccount(@RequestParam UUID userId, @Valid @RequestBody PayoutAccountRequest request) {
        return ApiResponse.success(payoutService.createAccount(accessHelper.resolveMerchantId(userId), request));
    }

    @DeleteMapping("/payout-accounts/{accountId}")
    public ApiResponse<Void> deleteAccount(@RequestParam UUID userId, @PathVariable UUID accountId) {
        payoutService.deleteAccount(accessHelper.resolveMerchantId(userId), accountId);
        return ApiResponse.success(null);
    }

    @PutMapping("/payout-accounts/{accountId}/default")
    public ApiResponse<PayoutAccountResponse> setDefault(@RequestParam UUID userId, @PathVariable UUID accountId) {
        return ApiResponse.success(payoutService.setDefault(accessHelper.resolveMerchantId(userId), accountId));
    }

    @GetMapping("/preferences")
    public ApiResponse<MerchantPreferencesResponse> getPreferences(@RequestParam UUID userId) {
        return ApiResponse.success(payoutService.getPreferences(accessHelper.resolveMerchantId(userId)));
    }

    @PutMapping("/preferences")
    public ApiResponse<MerchantPreferencesResponse> updatePreferences(@RequestParam UUID userId, @Valid @RequestBody MerchantPreferencesRequest request) {
        return ApiResponse.success(payoutService.updatePreferences(accessHelper.resolveMerchantId(userId), request));
    }
}
