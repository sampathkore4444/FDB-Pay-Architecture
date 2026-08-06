package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.DiscountCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, UUID> {
    List<DiscountCode> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Optional<DiscountCode> findByMerchantIdAndCodeIgnoreCase(UUID merchantId, String code);
}
