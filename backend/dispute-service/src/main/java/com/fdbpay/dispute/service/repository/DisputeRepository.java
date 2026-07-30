package com.fdbpay.dispute.service.repository;

import com.fdbpay.dispute.service.model.Dispute;
import com.fdbpay.dispute.service.model.enums.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findByTransactionId(UUID transactionId);

    Page<Dispute> findByComplainantUserIdOrderByCreatedAtDesc(UUID complainantUserId, Pageable pageable);

    Page<Dispute> findByStatusOrderByCreatedAtDesc(DisputeStatus status, Pageable pageable);

    long countByStatus(DisputeStatus status);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM d.resolved_at - d.created_at) / 3600.0) FROM disputes d WHERE d.resolved_at IS NOT NULL", nativeQuery = true)
    Double avgResolutionHours();

    @Query("SELECT d FROM Dispute d ORDER BY d.createdAt DESC")
    Page<Dispute> findAllOrderByCreatedAtDesc(Pageable pageable);
}
