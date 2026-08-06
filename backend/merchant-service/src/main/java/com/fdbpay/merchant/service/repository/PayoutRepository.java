package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.Payout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    Page<Payout> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);
}
