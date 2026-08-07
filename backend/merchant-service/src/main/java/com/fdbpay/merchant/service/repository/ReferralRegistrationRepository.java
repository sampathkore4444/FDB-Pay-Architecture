package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.ReferralRegistration;
import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReferralRegistrationRepository extends JpaRepository<ReferralRegistration, UUID> {

    List<ReferralRegistration> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    long countByMerchantIdAndStatus(UUID merchantId, ActiveStatus status);

    long countByMerchantIdAndConvertedAtIsNotNull(UUID merchantId);
}
