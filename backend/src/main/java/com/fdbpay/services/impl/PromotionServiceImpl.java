package com.fdbpay.services.impl;

import com.fdbpay.services.PromotionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PromotionServiceImpl implements PromotionService {

    @Override
    public Map<String, Object> getActivePromotions(UUID userId) {
        return Map.of("promotions", java.util.List.of());
    }

    @Override
    public Map<String, Object> applyPromoCode(String userId, String promoCode, Long transactionAmount) {
        log.info("Applying promo code: userId={}, code={}", userId, promoCode);
        return Map.of("applied", true, "discount", 0L);
    }

    @Override
    public Map<String, Object> getCashback(UUID userId, int page, int size) {
        return Map.of("cashbacks", java.util.List.of(), "total", 0);
    }

    @Override
    public void createPromotion(Map<String, Object> promotionDetails) {
        log.info("Promotion created: {}", promotionDetails);
    }

    @Override
    public void deactivatePromotion(String promotionId) {
        log.info("Promotion deactivated: {}", promotionId);
    }
}
