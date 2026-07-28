package com.fdbpay.promotions.service.repository;

import com.fdbpay.promotions.service.model.CashbackTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CashbackTransactionRepository extends JpaRepository<CashbackTransaction, UUID> {

    Page<CashbackTransaction> findByCashbackWalletIdOrderByCreatedAtDesc(UUID cashbackWalletId, Pageable pageable);
}
