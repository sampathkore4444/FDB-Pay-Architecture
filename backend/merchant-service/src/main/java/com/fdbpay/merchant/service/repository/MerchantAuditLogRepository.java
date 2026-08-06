package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.MerchantAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MerchantAuditLogRepository extends JpaRepository<MerchantAuditLog, UUID> {
    List<MerchantAuditLog> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<MerchantAuditLog> findByMerchantIdAndStaffIdOrderByCreatedAtDesc(UUID merchantId, UUID staffId);
}
