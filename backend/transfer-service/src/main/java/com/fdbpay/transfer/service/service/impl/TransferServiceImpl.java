package com.fdbpay.transfer.service.service.impl;

import com.fdbpay.shared.constants.AppConstants;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.event.TransactionEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.transfer.service.dto.request.TransferRequest;
import com.fdbpay.transfer.service.dto.response.TransactionResponse;
import com.fdbpay.transfer.service.model.Transaction;
import com.fdbpay.transfer.service.model.enums.TransactionStatus;
import com.fdbpay.transfer.service.repository.TransactionRepository;
import com.fdbpay.transfer.service.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransactionRepository transactionRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String WALLET_SERVICE_BASE = "http://wallet-service/wallet";

    @Override
    @Transactional
    public TransactionResponse initiateTransfer(TransferRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .type(request.getType())
                .status(TransactionStatus.PENDING)
                .amount(request.getAmount())
                .currency("MMK")
                .description(request.getDescription())
                .createdAt(OffsetDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Transfer initiated: transactionId={}, type={}, amount={}",
                transaction.getId(), transaction.getType(), transaction.getAmount());

        try {
            processTransfer(transaction, request);
        } catch (Exception e) {
            log.error("Transfer processing failed: transactionId={}", transaction.getId(), e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            publishEvent(transaction, "FAILED");
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR, "Transfer processing failed: " + e.getMessage());
        }

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
    public TransactionResponse confirmTransfer(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId.toString()));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Cannot confirm transaction in status: " + transaction.getStatus());
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(OffsetDateTime.now());
        transaction = transactionRepository.save(transaction);

        publishEvent(transaction, "COMPLETED");
        log.info("Transfer confirmed: transactionId={}", transactionId);

        return mapToResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse cancelTransfer(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId.toString()));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Cannot cancel transaction in status: " + transaction.getStatus());
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setFailureReason("Cancelled by user");
        transaction = transactionRepository.save(transaction);

        publishEvent(transaction, "CANCELLED");
        log.info("Transfer cancelled: transactionId={}", transactionId);

        return mapToResponse(transaction);
    }

    @Override
    public Page<TransactionResponse> getHistory(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionRepository
                .findBySenderWalletIdOrderByCreatedAtDesc(userId, pageRequest);
        return transactions.map(this::mapToResponse);
    }

    private void processTransfer(Transaction transaction, TransferRequest request) {
        UUID txnId = transaction.getId();
        String description = request.getDescription() != null ? request.getDescription() : "Transfer";

        WebClient webClient = webClientBuilder.build();

        Map<String, Object> debitRequest = Map.of(
                "walletId", transaction.getSenderWalletId().toString(),
                "amount", transaction.getAmount(),
                "description", description,
                "txnId", txnId.toString()
        );

        webClient.post()
                .uri(WALLET_SERVICE_BASE + "/debit")
                .bodyValue(debitRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map<String, Object> creditRequest = Map.of(
                "walletId", transaction.getReceiverWalletId().toString(),
                "amount", transaction.getAmount(),
                "description", description,
                "txnId", txnId.toString()
        );

        webClient.post()
                .uri(WALLET_SERVICE_BASE + "/credit")
                .bodyValue(creditRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(OffsetDateTime.now());
        transactionRepository.save(transaction);

        publishEvent(transaction, "COMPLETED");
        log.info("Transfer completed: transactionId={}, amount={}", txnId, transaction.getAmount());
    }

    private void checkIdempotency(String idempotencyKey) {
        String cacheKey = AppConstants.IDEMPOTENCY_CACHE_PREFIX + idempotencyKey;
        Boolean exists = redisTemplate.hasKey(cacheKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BusinessException(ErrorCodes.DUPLICATE_TRANSACTION,
                    "Duplicate transaction detected for idempotency key: " + idempotencyKey);
        }
        redisTemplate.opsForValue().set(cacheKey, "1", 24, TimeUnit.HOURS);
    }

    private void publishEvent(Transaction transaction, String status) {
        try {
            TransactionEvent event = TransactionEvent.builder()
                    .transactionId(transaction.getId())
                    .type(transaction.getType().name())
                    .status(status)
                    .senderWalletId(transaction.getSenderWalletId())
                    .receiverWalletId(transaction.getReceiverWalletId())
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .timestamp(OffsetDateTime.now())
                    .build();
            kafkaTemplate.send(AppConstants.TOPIC_TXN_COMPLETED, transaction.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish transaction event: transactionId={}", transaction.getId(), e);
        }
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .senderWalletId(transaction.getSenderWalletId())
                .receiverWalletId(transaction.getReceiverWalletId())
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .metadata(transaction.getMetadata())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .failureReason(transaction.getFailureReason())
                .build();
    }
}
