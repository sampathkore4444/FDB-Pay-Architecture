package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.RiskAlert;
import com.fdbpay.merchant.service.model.enums.RiskAlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskAlertRepository extends JpaRepository<RiskAlert, UUID> {

    List<RiskAlert> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<RiskAlert> findByMerchantIdAndStatus(UUID merchantId, RiskAlertStatus status);

    Optional<RiskAlert> findTopByMerchantIdAndAlertTypeAndStatusOrderByCreatedAtDesc(
            UUID merchantId, String alertType, RiskAlertStatus status);
}
