package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.FinancingApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FinancingApplicationRepository extends JpaRepository<FinancingApplication, UUID> {

    List<FinancingApplication> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
