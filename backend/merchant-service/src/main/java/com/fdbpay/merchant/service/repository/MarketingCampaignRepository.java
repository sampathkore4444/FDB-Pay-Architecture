package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.MarketingCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarketingCampaignRepository extends JpaRepository<MarketingCampaign, UUID> {
    List<MarketingCampaign> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
