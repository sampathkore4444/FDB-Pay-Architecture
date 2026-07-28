package com.fdbpay.repositories;

import com.fdbpay.models.entity.Transaction;
import com.fdbpay.models.enums.TransactionStatus;
import com.fdbpay.models.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findBySenderWalletIdOrderByCreatedAtDesc(UUID senderWalletId, Pageable pageable);

    Page<Transaction> findByReceiverWalletIdOrderByCreatedAtDesc(UUID receiverWalletId, Pageable pageable);

    Page<Transaction> findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc(
            UUID senderWalletId, UUID receiverWalletId, Pageable pageable);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    Page<Transaction> findByType(TransactionType type, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :start AND :end AND t.status = 'COMPLETED'")
    List<Transaction> findCompletedTransactionsInPeriod(
            @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.createdAt BETWEEN :start AND :end")
    List<Transaction> findTransactionsByWalletInPeriod(
            @Param("walletId") UUID walletId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);

    long countByStatusAndCreatedAtBetween(TransactionStatus status, OffsetDateTime start, OffsetDateTime end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'COMPLETED' AND t.createdAt BETWEEN :start AND :end")
    Long sumAmountByStatusAndPeriod(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
