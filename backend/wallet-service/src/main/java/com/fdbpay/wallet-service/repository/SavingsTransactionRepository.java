package com.fdbpay.wallet.service.repository;

import com.fdbpay.wallet.service.model.SavingsTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SavingsTransactionRepository extends JpaRepository<SavingsTransaction, UUID> {

    Page<SavingsTransaction> findByPocketIdOrderByCreatedAtDesc(UUID pocketId, Pageable pageable);
}
