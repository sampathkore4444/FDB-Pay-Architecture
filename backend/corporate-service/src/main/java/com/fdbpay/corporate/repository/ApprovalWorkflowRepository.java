package com.fdbpay.corporate.repository;

import com.fdbpay.corporate.model.ApprovalWorkflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, UUID> {

    List<ApprovalWorkflow> findByBulkDisbursementId(UUID bulkDisbursementId);

    Page<ApprovalWorkflow> findByApproverUserIdAndStatusOrderByCreatedAtDesc(UUID approverUserId, String status, Pageable pageable);
}
