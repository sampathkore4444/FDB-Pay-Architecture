package com.fdbpay.promotions.service.service;

import com.fdbpay.promotions.service.dto.request.ApplyPromotionRequest;
import com.fdbpay.promotions.service.dto.request.CreatePromotionRequest;
import com.fdbpay.promotions.service.dto.request.RedeemCashbackRequest;
import com.fdbpay.promotions.service.dto.response.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface PromotionsService {

    PromotionResponse createPromotion(CreatePromotionRequest request);

    List<PromotionResponse> getActivePromotions(UUID userId);

    PromotionValidationResponse validatePromoCode(String promoCode, Long amount, UUID userId);

    PromotionUsageResponse applyPromotion(UUID userId, ApplyPromotionRequest request, UUID transactionId);

    CashbackWalletResponse getCashbackWallet(UUID userId);

    CashbackWalletResponse redeemCashback(UUID userId, RedeemCashbackRequest request);

    Page<PromotionResponse> getMyPromotions(UUID userId, int page, int size);

    void deactivatePromotion(UUID promotionId);
}
