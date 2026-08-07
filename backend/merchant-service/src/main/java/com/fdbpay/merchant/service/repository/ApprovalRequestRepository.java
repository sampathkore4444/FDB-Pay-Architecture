package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.ApprovalRequest;
import com.fdbpay.merchant.service.model.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    List<ApprovalRequest> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<ApprovalRequest> findByMerchantIdAndStatusOrderByCreatedAtDesc(UUID merchantId, ApprovalStatus status);
}
