package com.fdbpay.remittance.service.repository;

import com.fdbpay.remittance.service.model.Remittance;
import com.fdbpay.remittance.service.model.enums.RemittanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RemittanceRepository extends JpaRepository<Remittance, UUID> {

    Page<Remittance> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    List<Remittance> findByCorridor(String corridor);

    List<Remittance> findByStatus(RemittanceStatus status);

    Optional<Remittance> findByPartnerRef(String partnerRef);

    Optional<Remittance> findByReferenceNumber(String referenceNumber);
}
