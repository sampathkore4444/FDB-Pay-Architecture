package com.fdbpay.transfer.service.repository;

import com.fdbpay.transfer.service.model.Transaction;
import com.fdbpay.transfer.service.model.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    Page<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status, Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            TransactionStatus status, OffsetDateTime from, OffsetDateTime to);

    List<Transaction> findByReceiverWalletIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID walletId, TransactionStatus status, OffsetDateTime from, OffsetDateTime to);

    List<Transaction> findBySenderWalletIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID walletId, TransactionStatus status, OffsetDateTime from, OffsetDateTime to);
}
