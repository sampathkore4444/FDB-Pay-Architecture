package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.MerchantPreferencesRequest;
import com.fdbpay.merchant.service.dto.request.PayoutAccountRequest;
import com.fdbpay.merchant.service.dto.request.PayoutRequest;
import com.fdbpay.merchant.service.dto.response.ContractResponse;
import com.fdbpay.merchant.service.dto.response.MerchantPreferencesResponse;
import com.fdbpay.merchant.service.dto.response.PayoutAccountResponse;
import com.fdbpay.merchant.service.dto.response.PayoutResponse;
import com.fdbpay.merchant.service.service.PayoutService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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

    @PostMapping("/payouts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PayoutResponse> requestPayout(@RequestParam UUID userId, @Valid @RequestBody PayoutRequest request) {
        return ApiResponse.success(payoutService.requestPayout(accessHelper.resolveMerchantId(userId), request));
    }

    @GetMapping("/payouts")
    public ApiResponse<Page<PayoutResponse>> listPayouts(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(payoutService.listPayouts(accessHelper.resolveMerchantId(userId), page, size));
    }

    @GetMapping("/payouts/available-balance")
    public ApiResponse<Map<String, Long>> availableBalance(@RequestParam UUID userId) {
        return ApiResponse.success(Map.of("balanceAvailable", payoutService.getAvailableBalance(accessHelper.resolveMerchantId(userId))));
    }

    @GetMapping("/contract")
    public ApiResponse<ContractResponse> contract(@RequestParam UUID userId) {
        return ApiResponse.success(payoutService.getContract(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping("/payouts/{payoutId}/approve")
    public ApiResponse<PayoutResponse> approve(@RequestParam UUID userId, @PathVariable UUID payoutId,
                                               @RequestParam String reviewer) {
        return ApiResponse.success(payoutService.approvePayout(accessHelper.resolveMerchantId(userId), payoutId, reviewer));
    }

    @PostMapping("/payouts/{payoutId}/reject")
    public ApiResponse<PayoutResponse> reject(@RequestParam UUID userId, @PathVariable UUID payoutId,
                                              @RequestParam String reviewer) {
        return ApiResponse.success(payoutService.rejectPayout(accessHelper.resolveMerchantId(userId), payoutId, reviewer));
    }
}
