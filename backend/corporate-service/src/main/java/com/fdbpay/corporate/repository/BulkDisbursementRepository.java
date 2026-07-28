package com.fdbpay.corporate.repository;

import com.fdbpay.corporate.model.BulkDisbursement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BulkDisbursementRepository extends JpaRepository<BulkDisbursement, UUID> {

    Page<BulkDisbursement> findByCorporateUserIdOrderByCreatedAtDesc(UUID corporateUserId, Pageable pageable);

    Optional<BulkDisbursement> findByIdAndCorporateUserId(UUID id, UUID corporateUserId);
}
