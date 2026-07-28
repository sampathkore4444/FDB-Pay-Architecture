package com.fdbpay.repositories;

import com.fdbpay.models.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    List<LedgerEntry> findByTxnId(UUID txnId);

    List<LedgerEntry> findByWalletIdAndCreatedAtBetween(UUID walletId, java.time.OffsetDateTime start, java.time.OffsetDateTime end);
}
