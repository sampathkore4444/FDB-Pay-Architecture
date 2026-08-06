package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
