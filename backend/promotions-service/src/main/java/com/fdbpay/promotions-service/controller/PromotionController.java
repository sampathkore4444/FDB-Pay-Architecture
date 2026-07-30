package com.fdbpay.promotions.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.promotions.service.dto.request.ApplyPromotionRequest;
import com.fdbpay.promotions.service.dto.request.CreatePromotionRequest;
import com.fdbpay.promotions.service.dto.request.RedeemCashbackRequest;
import com.fdbpay.promotions.service.dto.response.*;
import com.fdbpay.promotions.service.service.PromotionsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionsService promotionsService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PromotionResponse> createPromotion(
            @Valid @RequestBody CreatePromotionRequest request) {
        return ApiResponse.success(promotionsService.createPromotion(request));
    }

    @GetMapping("/active")
    public ApiResponse<List<PromotionResponse>> getActivePromotions(
            @RequestParam(required = false) UUID userId) {
        return ApiResponse.success(promotionsService.getActivePromotions(userId));
    }

    @PostMapping("/validate")
    public ApiResponse<PromotionValidationResponse> validatePromoCode(
            @RequestParam String promoCode,
            @RequestParam Long amount,
            @RequestParam UUID userId) {
        return ApiResponse.success(promotionsService.validatePromoCode(promoCode, amount, userId));
    }

    @PostMapping("/apply")
    public ApiResponse<PromotionUsageResponse> applyPromotion(
            @RequestParam UUID userId,
            @Valid @RequestBody ApplyPromotionRequest request) {
        return ApiResponse.success(promotionsService.applyPromotion(userId, request, UUID.randomUUID()));
    }

    @GetMapping("/cashback-wallet")
    public ApiResponse<CashbackWalletResponse> getCashbackWallet(@RequestParam UUID userId) {
        return ApiResponse.success(promotionsService.getCashbackWallet(userId));
    }

    @PostMapping("/cashback-redeem")
    public ApiResponse<CashbackWalletResponse> redeemCashback(
            @RequestParam UUID userId,
            @Valid @RequestBody RedeemCashbackRequest request) {
        return ApiResponse.success(promotionsService.redeemCashback(userId, request));
    }

    @GetMapping("/my")
    public ApiResponse<Page<PromotionResponse>> getMyPromotions(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(promotionsService.getMyPromotions(userId, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivatePromotion(@PathVariable UUID id) {
        promotionsService.deactivatePromotion(id);
        return ApiResponse.success(null);
    }
}
