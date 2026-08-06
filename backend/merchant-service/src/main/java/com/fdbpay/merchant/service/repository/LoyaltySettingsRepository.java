package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.LoyaltySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LoyaltySettingsRepository extends JpaRepository<LoyaltySettings, UUID> {
    Optional<LoyaltySettings> findByMerchantId(UUID merchantId);
}
