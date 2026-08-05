package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.Chargeback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChargebackRepository extends JpaRepository<Chargeback, UUID> {

    List<Chargeback> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
