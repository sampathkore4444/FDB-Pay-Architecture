package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.CashbackCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CashbackCampaignRepository extends JpaRepository<CashbackCampaign, UUID> {
    List<CashbackCampaign> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
