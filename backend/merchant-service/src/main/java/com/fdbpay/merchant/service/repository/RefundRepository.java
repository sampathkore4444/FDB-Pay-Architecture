package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.Refund;
import com.fdbpay.merchant.service.model.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<Refund> findByMerchantIdAndStatus(UUID merchantId, RefundStatus status);

    List<Refund> findByOrderId(UUID orderId);
}
