package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.ChargebackEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChargebackEvidenceRepository extends JpaRepository<ChargebackEvidence, UUID> {

    List<ChargebackEvidence> findByChargebackIdOrderByCreatedAtDesc(UUID chargebackId);

    long countByChargebackId(UUID chargebackId);
}
