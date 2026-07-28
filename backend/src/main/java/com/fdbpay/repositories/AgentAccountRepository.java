package com.fdbpay.repositories;

import com.fdbpay.models.entity.AgentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentAccountRepository extends JpaRepository<AgentAccount, UUID> {

    Optional<AgentAccount> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
