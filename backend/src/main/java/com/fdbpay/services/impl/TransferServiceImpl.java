package com.fdbpay.services.impl;

import com.fdbpay.common.constants.ErrorCodes;
import com.fdbpay.common.exceptions.BusinessException;
import com.fdbpay.common.exceptions.InsufficientBalanceException;
import com.fdbpay.common.exceptions.ResourceNotFoundException;
import com.fdbpay.events.KafkaEventPublisher;
import com.fdbpay.models.entity.Transaction;
import com.fdbpay.models.entity.User;
import com.fdbpay.models.entity.Wallet;
import com.fdbpay.models.enums.TransactionStatus;
import com.fdbpay.models.enums.TransactionType;
import com.fdbpay.models.enums.WalletStatus;
import com.fdbpay.repositories.TransactionRepository;
import com.fdbpay.repositories.UserRepository;
import com.fdbpay.repositories.WalletRepository;
import com.fdbpay.schemas.request.TransferRequest;
import com.fdbpay.schemas.response.TransactionResponse;
import com.fdbpay.services.TransferService;
import com.fdbpay.services.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final KafkaEventPublisher eventPublisher;

    @Override
    @Transactional
    public TransactionResponse initiateTransfer(UUID senderUserId, TransferRequest request) {
        Transaction existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing != null) {
            return mapToResponse(existing);
        }

        Wallet senderWallet = walletRepository.findByUserId(senderUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", senderUserId.toString()));

        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.ACCOUNT_SUSPENDED, "Sender wallet is not active");
        }

        Wallet receiverWallet = resolveRecipient(request.getRecipientIdentifier(), request.getRecipientType());

        if (senderWallet.getAvailableBalance() < request.getAmount()) {
            throw new InsufficientBalanceException(senderWallet.getAvailableBalance(), request.getAmount());
        }

        UUID txnId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .type(TransactionType.valueOf(request.getType() != null ? request.getType() : "P2P"))
                .status(TransactionStatus.COMPLETED)
                .senderWallet(senderWallet)
                .receiverWallet(receiverWallet)
                .amount(request.getAmount())
                .fee(0L)
                .currency("MMK")
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .completedAt(OffsetDateTime.now())
                .build();

        walletService.debitWallet(senderWallet.getId(), request.getAmount(), "P2P Transfer", txnId);
        walletService.creditWallet(receiverWallet.getId(), request.getAmount(), "P2P Transfer", txnId);

        transaction = transactionRepository.save(transaction);

        eventPublisher.publishTransactionCompleted(transaction);

        log.info("Transfer completed: id={}, amount={}, sender={}, receiver={}",
                txnId, request.getAmount(), senderUserId, receiverWallet.getUser().getId());

        return mapToResponse(transaction);
    }

    @Override
    public TransactionResponse getTransferStatus(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId.toString()));
        return mapToResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse confirmTransfer(UUID transactionId, String pin) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId.toString()));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Transaction is not in PENDING status");
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(OffsetDateTime.now());
        transactionRepository.save(transaction);

        return mapToResponse(transaction);
    }

    @Override
    public void cancelTransfer(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId.toString()));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Cannot cancel a non-pending transaction");
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transactionRepository.save(transaction);
    }

    @Override
    public Map<String, Object> getTransferHistory(UUID userId, Pageable pageable) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId.toString()));

        Page<Transaction> transactions = transactionRepository
                .findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc(
                        wallet.getId(), wallet.getId(), pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("transactions", transactions.getContent());
        result.put("page", transactions.getNumber());
        result.put("size", transactions.getSize());
        result.put("total", transactions.getTotalElements());
        result.put("totalPages", transactions.getTotalPages());
        return result;
    }

    @Override
    public void processScheduledTransfers() {
        log.info("Processing scheduled transfers...");
    }

    private Wallet resolveRecipient(String identifier, String type) {
        if ("PHONE".equalsIgnoreCase(type) || type == null) {
            User recipient = userRepository.findByPhone(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Recipient not found: " + identifier));
            return walletRepository.findByUserId(recipient.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recipient wallet not found"));
        }
        return walletRepository.findById(UUID.fromString(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", identifier));
    }

    private TransactionResponse mapToResponse(Transaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .idempotencyKey(txn.getIdempotencyKey())
                .type(txn.getType().name())
                .status(txn.getStatus().name())
                .senderWalletId(txn.getSenderWallet() != null ? txn.getSenderWallet().getId() : null)
                .receiverWalletId(txn.getReceiverWallet() != null ? txn.getReceiverWallet().getId() : null)
                .amount(txn.getAmount())
                .fee(txn.getFee())
                .currency(txn.getCurrency())
                .description(txn.getDescription())
                .metadata(txn.getMetadata())
                .createdAt(txn.getCreatedAt())
                .completedAt(txn.getCompletedAt())
                .failureReason(txn.getFailureReason())
                .build();
    }
}
