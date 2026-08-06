package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FraudRuleRepository extends JpaRepository<FraudRule, UUID> {
    List<FraudRule> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
