package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.MerchantReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MerchantReviewRepository extends JpaRepository<MerchantReview, UUID> {
    List<MerchantReview> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
