package com.fdbpay.dispute.service.repository;

import com.fdbpay.dispute.service.model.DisputeEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisputeEvidenceRepository extends JpaRepository<DisputeEvidence, UUID> {

    List<DisputeEvidence> findByDisputeIdOrderByCreatedAtAsc(UUID disputeId);
}
