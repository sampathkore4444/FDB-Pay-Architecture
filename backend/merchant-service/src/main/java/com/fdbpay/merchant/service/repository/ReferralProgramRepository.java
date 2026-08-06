package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.ReferralProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReferralProgramRepository extends JpaRepository<ReferralProgram, UUID> {
    List<ReferralProgram> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
