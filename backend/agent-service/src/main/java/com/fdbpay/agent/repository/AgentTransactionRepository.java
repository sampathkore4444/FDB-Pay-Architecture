package com.fdbpay.agent.repository;

import com.fdbpay.agent.model.AgentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AgentTransactionRepository extends JpaRepository<AgentTransaction, UUID> {

    Page<AgentTransaction> findByAgentUserIdOrderByCreatedAtDesc(UUID agentUserId, Pageable pageable);
}
