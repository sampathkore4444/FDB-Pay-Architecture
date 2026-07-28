package com.fdbpay.agent.repository;

import com.fdbpay.agent.model.CommissionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommissionRecordRepository extends JpaRepository<CommissionRecord, UUID> {

    Page<CommissionRecord> findByAgentUserIdOrderByCreatedAtDesc(UUID agentUserId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM CommissionRecord c WHERE c.agentUserId = :agentUserId AND c.status = :status")
    Long sumCommissionAmountByAgentUserIdAndStatus(@Param("agentUserId") UUID agentUserId, @Param("status") String status);

    List<CommissionRecord> findByAgentUserIdAndStatusOrderByCreatedAtDesc(UUID agentUserId, String status);
}
