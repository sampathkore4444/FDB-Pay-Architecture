package com.fdbpay.kyc.service.repository;

import com.fdbpay.kyc.service.model.KycAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KycAuditRepository extends JpaRepository<KycAudit, UUID> {
}
