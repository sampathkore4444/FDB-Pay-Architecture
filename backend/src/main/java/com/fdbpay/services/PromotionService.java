package com.fdbpay.services;

import java.util.Map;
import java.util.UUID;

public interface PromotionService {

    Map<String, Object> getActivePromotions(UUID userId);

    Map<String, Object> applyPromoCode(String userId, String promoCode, Long transactionAmount);

    Map<String, Object> getCashback(UUID userId, int page, int size);

    void createPromotion(Map<String, Object> promotionDetails);

    void deactivatePromotion(String promotionId);
}
