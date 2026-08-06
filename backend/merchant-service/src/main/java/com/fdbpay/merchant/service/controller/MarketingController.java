package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.AbTestRequest;
import com.fdbpay.merchant.service.dto.request.CashbackCampaignRequest;
import com.fdbpay.merchant.service.dto.request.DiscountCodeRequest;
import com.fdbpay.merchant.service.dto.request.LoyaltySettingsRequest;
import com.fdbpay.merchant.service.dto.request.MarketingCampaignRequest;
import com.fdbpay.merchant.service.dto.request.ReferralProgramRequest;
import com.fdbpay.merchant.service.dto.response.CashbackCampaignResponse;
import com.fdbpay.merchant.service.dto.response.DiscountCodeResponse;
import com.fdbpay.merchant.service.dto.response.LoyaltySettingsResponse;
import com.fdbpay.merchant.service.dto.response.MarketingCampaignResponse;
import com.fdbpay.merchant.service.dto.response.ReferralProgramResponse;
import com.fdbpay.merchant.service.service.MarketingService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MarketingController {

    private final MarketingService marketingService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping("/discount-codes")
    public ApiResponse<List<DiscountCodeResponse>> listDiscountCodes(@RequestParam UUID userId) {
        return ApiResponse.success(marketingService.listDiscountCodes(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping("/discount-codes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DiscountCodeResponse> createDiscountCode(@RequestParam UUID userId, @Valid @RequestBody DiscountCodeRequest request) {
        return ApiResponse.success(marketingService.createDiscountCode(accessHelper.resolveMerchantId(userId), request));
    }

    @PutMapping("/discount-codes/{codeId}/toggle")
    public ApiResponse<DiscountCodeResponse> toggleDiscountCode(@RequestParam UUID userId, @PathVariable UUID codeId) {
        return ApiResponse.success(marketingService.toggleDiscountCode(accessHelper.resolveMerchantId(userId), codeId));
    }

    @DeleteMapping("/discount-codes/{codeId}")
    public ApiResponse<Void> deleteDiscountCode(@RequestParam UUID userId, @PathVariable UUID codeId) {
        marketingService.deleteDiscountCode(accessHelper.resolveMerchantId(userId), codeId);
        return ApiResponse.success(null);
    }

    @GetMapping("/discount-codes/validate")
    public ApiResponse<DiscountCodeResponse> validateDiscountCode(
            @RequestParam UUID userId,
            @RequestParam String code,
            @RequestParam(required = false) Long amount) {
        return ApiResponse.success(marketingService.validateDiscountCode(accessHelper.resolveMerchantId(userId), code, amount));
    }

    @PostMapping("/discount-codes/ab-test")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<DiscountCodeResponse>> createAbTest(@RequestParam UUID userId, @Valid @RequestBody AbTestRequest request) {
        return ApiResponse.success(marketingService.createAbTest(accessHelper.resolveMerchantId(userId), request));
    }

    @GetMapping("/marketing-campaigns")
    public ApiResponse<List<MarketingCampaignResponse>> listMarketingCampaigns(@RequestParam UUID userId) {
        return ApiResponse.success(marketingService.listMarketingCampaigns(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping("/marketing-campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MarketingCampaignResponse> createMarketingCampaign(@RequestParam UUID userId, @Valid @RequestBody MarketingCampaignRequest request) {
        return ApiResponse.success(marketingService.createMarketingCampaign(accessHelper.resolveMerchantId(userId), request));
    }

    @PutMapping("/marketing-campaigns/{campaignId}/toggle")
    public ApiResponse<MarketingCampaignResponse> toggleMarketingCampaign(@RequestParam UUID userId, @PathVariable UUID campaignId) {
        return ApiResponse.success(marketingService.toggleMarketingCampaign(accessHelper.resolveMerchantId(userId), campaignId));
    }

    @DeleteMapping("/marketing-campaigns/{campaignId}")
    public ApiResponse<Void> deleteMarketingCampaign(@RequestParam UUID userId, @PathVariable UUID campaignId) {
        marketingService.deleteMarketingCampaign(accessHelper.resolveMerchantId(userId), campaignId);
        return ApiResponse.success(null);
    }

    @GetMapping("/cashback-campaigns")
    public ApiResponse<List<CashbackCampaignResponse>> listCampaigns(@RequestParam UUID userId) {
        return ApiResponse.success(marketingService.listCampaigns(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping("/cashback-campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CashbackCampaignResponse> createCampaign(@RequestParam UUID userId, @Valid @RequestBody CashbackCampaignRequest request) {
        return ApiResponse.success(marketingService.createCampaign(accessHelper.resolveMerchantId(userId), request));
    }

    @PutMapping("/cashback-campaigns/{campaignId}/toggle")
    public ApiResponse<CashbackCampaignResponse> toggleCampaign(@RequestParam UUID userId, @PathVariable UUID campaignId) {
        return ApiResponse.success(marketingService.toggleCampaign(accessHelper.resolveMerchantId(userId), campaignId));
    }

    @DeleteMapping("/cashback-campaigns/{campaignId}")
    public ApiResponse<Void> deleteCampaign(@RequestParam UUID userId, @PathVariable UUID campaignId) {
        marketingService.deleteCampaign(accessHelper.resolveMerchantId(userId), campaignId);
        return ApiResponse.success(null);
    }

    @GetMapping("/referral")
    public ApiResponse<List<ReferralProgramResponse>> listReferralPrograms(@RequestParam UUID userId) {
        return ApiResponse.success(marketingService.listReferralPrograms(accessHelper.resolveMerchantId(userId)));
    }

    @GetMapping("/referral/code")
    public ApiResponse<Map<String, String>> generateReferralCode() {
        return ApiResponse.success(Map.of("code", marketingService.generateReferralCode()));
    }

    @PostMapping("/referral")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReferralProgramResponse> createReferralProgram(@RequestParam UUID userId, @Valid @RequestBody ReferralProgramRequest request) {
        return ApiResponse.success(marketingService.createReferralProgram(accessHelper.resolveMerchantId(userId), request));
    }

    @PutMapping("/referral/{programId}/toggle")
    public ApiResponse<ReferralProgramResponse> toggleReferralProgram(@RequestParam UUID userId, @PathVariable UUID programId) {
        return ApiResponse.success(marketingService.toggleReferralProgram(accessHelper.resolveMerchantId(userId), programId));
    }

    @DeleteMapping("/referral/{programId}")
    public ApiResponse<Void> deleteReferralProgram(@RequestParam UUID userId, @PathVariable UUID programId) {
        marketingService.deleteReferralProgram(accessHelper.resolveMerchantId(userId), programId);
        return ApiResponse.success(null);
    }

    @GetMapping("/loyalty")
    public ApiResponse<LoyaltySettingsResponse> getLoyalty(@RequestParam UUID userId) {
        return ApiResponse.success(marketingService.getLoyaltySettings(accessHelper.resolveMerchantId(userId)));
    }

    @PutMapping("/loyalty")
    public ApiResponse<LoyaltySettingsResponse> updateLoyalty(@RequestParam UUID userId, @Valid @RequestBody LoyaltySettingsRequest request) {
        return ApiResponse.success(marketingService.updateLoyaltySettings(accessHelper.resolveMerchantId(userId), request));
    }
}
