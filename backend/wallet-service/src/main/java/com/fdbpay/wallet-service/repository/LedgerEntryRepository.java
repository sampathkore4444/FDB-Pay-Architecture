package com.fdbpay.wallet.service.repository;

import com.fdbpay.wallet.service.model.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    List<LedgerEntry> findByTxnId(UUID txnId);

    Optional<LedgerEntry> findFirstByWalletIdAndTxnIdOrderByCreatedAtDesc(UUID walletId, UUID txnId);
}
