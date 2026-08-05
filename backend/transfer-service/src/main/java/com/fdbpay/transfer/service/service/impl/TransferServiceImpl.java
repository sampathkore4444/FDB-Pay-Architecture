package com.fdbpay.transfer.service.service.impl;

import com.fdbpay.shared.constants.AppConstants;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.event.TransactionEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.transfer.service.dto.request.TransferRequest;
import com.fdbpay.transfer.service.dto.request.ChargeRequest;
import com.fdbpay.transfer.service.dto.request.BulkRefundRequest;
import com.fdbpay.transfer.service.dto.request.BulkVoidRequest;
import com.fdbpay.transfer.service.dto.response.BulkOperationResponse;
import com.fdbpay.transfer.service.dto.response.BulkOperationResult;
import com.fdbpay.transfer.service.dto.response.TransactionResponse;
import com.fdbpay.transfer.service.dto.response.analytics.FeeBreakdown;
import com.fdbpay.transfer.service.dto.response.analytics.GrossByType;
import com.fdbpay.transfer.service.dto.response.analytics.MerchantStatement;
import com.fdbpay.transfer.service.dto.response.analytics.RollingReserveInfo;
import com.fdbpay.transfer.service.model.Transaction;
import com.fdbpay.transfer.service.model.enums.TransactionStatus;
import com.fdbpay.transfer.service.model.enums.TransactionType;
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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    public TransactionResponse initiateTransfer(UUID userId, TransferRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        UUID senderWalletId = getWalletIdByUserId(userId);
        Receiver receiver = resolveReceiver(request.getRecipientIdentifier());

        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .type(request.getType())
                .status(TransactionStatus.PENDING)
                .senderWalletId(senderWalletId)
                .receiverWalletId(receiver.walletId())
                .amount(request.getAmount())
                .currency("MMK")
                .description(request.getDescription())
                .createdAt(OffsetDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Transfer initiated: transactionId={}, type={}, amount={}, senderWalletId={}, receiverWalletId={}",
                transaction.getId(), transaction.getType(), transaction.getAmount(), senderWalletId, receiver.walletId());

        try {
            processTransfer(transaction, request, userId, receiver.userId());
        } catch (Exception e) {
            log.error("Transfer processing failed: transactionId={}", transaction.getId(), e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            publishEvent(transaction, "FAILED", userId, receiver.userId());
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

        publishEvent(transaction, "COMPLETED", null, null);
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

        publishEvent(transaction, "CANCELLED", null, null);
        log.info("Transfer cancelled: transactionId={}", transactionId);

        return mapToResponse(transaction);
    }

    @Override
    public Page<TransactionResponse> getHistory(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        UUID walletId = getWalletIdByUserId(userId);
        Page<Transaction> transactions = transactionRepository
                .findBySenderWalletIdOrderByCreatedAtDesc(walletId, pageRequest);
        return transactions.map(this::mapToResponse);
    }

    private void processTransfer(Transaction transaction, TransferRequest request, UUID senderUserId, UUID receiverUserId) {
        String description = request.getDescription() != null ? request.getDescription() : "Transfer";
        Transaction completed = executeWalletMovement(transaction, description);

        publishEvent(completed, "COMPLETED", senderUserId, receiverUserId);
        log.info("Transfer completed: transactionId={}, amount={}", transaction.getId(), transaction.getAmount());
    }

    private Transaction executeWalletMovement(Transaction transaction, String description) {
        UUID txnId = transaction.getId();
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
        return transactionRepository.save(transaction);
    }

    private UUID getWalletIdByUserId(UUID userId) {
        Map<?, ?> response = webClientBuilder.build()
                .get()
                .uri(WALLET_SERVICE_BASE + "?userId=" + userId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Sender wallet could not be resolved");
        }
        Object data = response.get("data");
        Object walletId = data != null ? ((Map<?, ?>) data).get("id") : null;
        if (walletId == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Sender wallet could not be resolved");
        }
        return UUID.fromString(walletId.toString());
    }

    private UUID getWalletOwnerUserId(UUID walletId) {
        try {
            Map<?, ?> response = webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder.scheme("http").host("wallet-service")
                            .path("/wallet/owner").queryParam("walletId", walletId).build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return null;
            }
            Object userId = response.get("data");
            return userId != null ? UUID.fromString(userId.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Receiver resolveReceiver(String recipientIdentifier) {
        if (recipientIdentifier == null || recipientIdentifier.isBlank()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Recipient identifier is required");
        }

        UUID identifierUuid;
        try {
            identifierUuid = UUID.fromString(recipientIdentifier);
        } catch (IllegalArgumentException e) {
            return resolveReceiverByPhone(recipientIdentifier);
        }

        UUID ownerUserId = getWalletOwnerUserId(identifierUuid);
        if (ownerUserId != null) {
            return new Receiver(identifierUuid, ownerUserId);
        }

        UUID walletId = getWalletIdByUserId(identifierUuid);
        return new Receiver(walletId, identifierUuid);
    }

    private Receiver resolveReceiverByPhone(String phone) {
        Map<?, ?> authResponse;
        try {
            String encodedPhone = URLEncoder.encode(phone, StandardCharsets.UTF_8);
            authResponse = webClientBuilder.build()
                    .get()
                    .uri(URI.create("http://auth-service/auth/user/by-phone?phone=" + encodedPhone))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Recipient not found: " + phone);
        }

        if (authResponse == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Recipient not found: " + phone);
        }
        Object data = authResponse.get("data");
        Object receiverUserId = data != null ? ((Map<?, ?>) data).get("id") : null;
        if (receiverUserId == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Recipient not found: " + phone);
        }
        UUID userId = UUID.fromString(receiverUserId.toString());
        return new Receiver(getWalletIdByUserId(userId), userId);
    }

    private record Receiver(UUID walletId, UUID userId) {
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

    private void publishEvent(Transaction transaction, String status, UUID senderUserId, UUID receiverUserId) {
        try {
            TransactionEvent event = TransactionEvent.builder()
                    .transactionId(transaction.getId())
                    .type(transaction.getType().name())
                    .status(status)
                    .senderWalletId(transaction.getSenderWalletId())
                    .receiverWalletId(transaction.getReceiverWalletId())
                    .senderUserId(senderUserId)
                    .receiverUserId(receiverUserId)
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .timestamp(OffsetDateTime.now())
                    .build();
            kafkaTemplate.send(AppConstants.TOPIC_TXN_COMPLETED, transaction.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish transaction event: transactionId={}", transaction.getId(), e);
        }
    }

    @Override
    @Transactional
    public TransactionResponse charge(UUID merchantUserId, ChargeRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        UUID merchantWalletId = getWalletIdByUserId(merchantUserId);
        Receiver customer = resolveReceiverByPhone(request.getCustomerPhone());

        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .type(TransactionType.CARD_PAYMENT)
                .status(TransactionStatus.PENDING)
                .senderWalletId(customer.walletId())
                .receiverWalletId(merchantWalletId)
                .amount(request.getAmount())
                .currency("MMK")
                .description(request.getDescription() != null ? request.getDescription() : "Card payment")
                .metadata(buildChargeMetadata(request))
                .createdAt(OffsetDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);

        String description = "Card payment " + safe(request.getCustomerName());
        try {
            Transaction completed = executeWalletMovement(transaction, description);
            publishEvent(completed, "COMPLETED", customer.userId(), merchantUserId);
            log.info("Card payment charged: transactionId={}, amount={}, merchantUserId={}",
                    completed.getId(), completed.getAmount(), merchantUserId);
            return mapToResponse(completed);
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR, "Card payment failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public BulkOperationResponse bulkRefund(UUID merchantUserId, BulkRefundRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        UUID merchantWalletId = getWalletIdByUserId(merchantUserId);
        List<Transaction> originals = transactionRepository.findAllById(request.getTransactionIds());

        List<BulkOperationResult> results = new ArrayList<>();
        int successCount = 0;

        for (Transaction original : originals) {
            UUID txnId = original.getId();
            if (!original.getReceiverWalletId().equals(merchantWalletId)) {
                results.add(BulkOperationResult.builder().transactionId(txnId).success(false)
                        .message("Transaction does not belong to this merchant").build());
                continue;
            }
            if (original.getStatus() != TransactionStatus.COMPLETED) {
                results.add(BulkOperationResult.builder().transactionId(txnId).success(false)
                        .message("Only completed transactions can be refunded. Status: " + original.getStatus()).build());
                continue;
            }

            String refundKey = request.getIdempotencyKey() + "-" + txnId;
            if (transactionRepository.existsByIdempotencyKey(refundKey)) {
                results.add(BulkOperationResult.builder().transactionId(txnId).success(false)
                        .message("Refund already processed for this transaction").build());
                continue;
            }

            Transaction refund = Transaction.builder()
                    .idempotencyKey(refundKey)
                    .type(TransactionType.REFUND)
                    .status(TransactionStatus.PENDING)
                    .senderWalletId(merchantWalletId)
                    .receiverWalletId(original.getSenderWalletId())
                    .amount(original.getAmount())
                    .currency("MMK")
                    .description(request.getReason() != null ? request.getReason() : "Refund")
                    .metadata("{\"method\":\"REFUND\",\"reason\":\"" + safe(request.getReason())
                            + "\",\"reasonCode\":\"" + safe(request.getReasonCode() == null ? null : request.getReasonCode().name())
                            + "\",\"note\":\"" + safe(request.getNote())
                            + "\",\"staffId\":\"" + safeId(request.getStaffId())
                            + "\",\"staffName\":\"" + safe(request.getStaffName()) + "\"}")
                    .parentTransactionId(txnId)
                    .createdAt(OffsetDateTime.now())
                    .build();
            refund = transactionRepository.save(refund);

            try {
                Transaction completed = executeWalletMovement(refund, "Refund");
                original.setStatus(TransactionStatus.REVERSED);
                original.setFailureReason("Refunded by merchant");
                transactionRepository.save(original);
                publishEvent(completed, "COMPLETED", merchantUserId, null);
                successCount++;
                results.add(BulkOperationResult.builder().transactionId(txnId).success(true).message("Refunded").build());
            } catch (Exception e) {
                refund.setStatus(TransactionStatus.FAILED);
                refund.setFailureReason(e.getMessage());
                transactionRepository.save(refund);
                results.add(BulkOperationResult.builder().transactionId(txnId).success(false)
                        .message("Refund failed: " + e.getMessage()).build());
            }
        }

        log.info("Bulk refund processed: merchantUserId={}, success={}, total={}",
                merchantUserId, successCount, results.size());
        return BulkOperationResponse.builder()
                .successCount(successCount)
                .failedCount(results.size() - successCount)
                .results(results)
                .build();
    }

    @Override
    @Transactional
    public BulkOperationResponse bulkVoid(UUID merchantUserId, BulkVoidRequest request) {
        UUID merchantWalletId = getWalletIdByUserId(merchantUserId);
        List<Transaction> targets = transactionRepository.findAllById(request.getTransactionIds());

        List<BulkOperationResult> results = new ArrayList<>();
        int successCount = 0;

        for (Transaction target : targets) {
            UUID txnId = target.getId();
            if (!target.getReceiverWalletId().equals(merchantWalletId)) {
                results.add(BulkOperationResult.builder().transactionId(txnId).success(false)
                        .message("Transaction does not belong to this merchant").build());
                continue;
            }
            if (target.getStatus() != TransactionStatus.PENDING) {
                results.add(BulkOperationResult.builder().transactionId(txnId).success(false)
                        .message("Only pending transactions can be voided. Status: " + target.getStatus()).build());
                continue;
            }

            target.setStatus(TransactionStatus.CANCELLED);
            target.setFailureReason("Voided by merchant");
            transactionRepository.save(target);
            successCount++;
            results.add(BulkOperationResult.builder().transactionId(txnId).success(true).message("Voided").build());
        }

        log.info("Bulk void processed: merchantUserId={}, success={}, total={}",
                merchantUserId, successCount, results.size());
        return BulkOperationResponse.builder()
                .successCount(successCount)
                .failedCount(results.size() - successCount)
                .results(results)
                .build();
    }

    @Override
    public MerchantStatement getStatement(UUID walletId, LocalDate from, LocalDate to,
                                          Integer rollingReservePercent, Integer rollingReservePeriodDays) {
        OffsetDateTime fromTime = (from != null ? from : LocalDate.now().minusDays(30))
                .atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        OffsetDateTime toTime = (to != null ? to : LocalDate.now())
                .plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC).minusNanos(1);

        List<Transaction> sales = transactionRepository
                .findByReceiverWalletIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                        walletId, TransactionStatus.COMPLETED, fromTime, toTime);
        List<Transaction> refunds = transactionRepository
                .findBySenderWalletIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                        walletId, TransactionStatus.COMPLETED, fromTime, toTime)
                .stream().filter(tx -> tx.getType() == TransactionType.REFUND)
                .collect(Collectors.toList());

        long totalVolume = sales.stream().mapToLong(Transaction::getAmount).sum();
        long totalFees = sales.stream().mapToLong(tx -> tx.getFee() == null ? 0 : tx.getFee()).sum();
        long cardFees = sales.stream()
                .filter(tx -> tx.getType() == TransactionType.CARD_PAYMENT)
                .mapToLong(tx -> tx.getFee() == null ? 0 : tx.getFee()).sum();
        long refundFees = refunds.stream().mapToLong(tx -> tx.getFee() == null ? 0 : tx.getFee()).sum();
        long refundAmount = refunds.stream().mapToLong(Transaction::getAmount).sum();

        Map<String, GrossByType> byType = new LinkedHashMap<>();
        for (Transaction tx : sales) {
            String key = tx.getType().name();
            GrossByType agg = byType.computeIfAbsent(key, k -> GrossByType.builder()
                    .type(k).count(0).volume(0L).fees(0L).build());
            agg.setCount(agg.getCount() + 1);
            agg.setVolume(agg.getVolume() + tx.getAmount());
            agg.setFees(agg.getFees() + (tx.getFee() == null ? 0 : tx.getFee()));
        }

        int percent = rollingReservePercent != null ? rollingReservePercent : 0;
        int periodDays = rollingReservePeriodDays != null ? rollingReservePeriodDays : 7;
        long heldThisPeriod = totalVolume * percent / 100;
        long daysInPeriod = java.time.Duration.between(fromTime, toTime).toDays() + 1;
        long releasedThisPeriod = heldThisPeriod == 0 ? 0
                : Math.min(heldThisPeriod, heldThisPeriod * Math.min(daysInPeriod, periodDays) / periodDays);

        return MerchantStatement.builder()
                .periodStart(fromTime)
                .periodEnd(toTime)
                .totalVolume(totalVolume)
                .transactionCount(sales.size())
                .feeBreakdown(FeeBreakdown.builder()
                        .transactionFees(totalFees)
                        .cardFees(cardFees)
                        .refundFees(refundFees)
                        .serviceFees(0L)
                        .build())
                .totalFees(totalFees)
                .netSales(totalVolume - totalFees)
                .refundCount(refunds.size())
                .refundAmount(refundAmount)
                .grossByType(new ArrayList<>(byType.values()))
                .rollingReserve(RollingReserveInfo.builder()
                        .percent(percent)
                        .heldThisPeriod(heldThisPeriod)
                        .releasedThisPeriod(releasedThisPeriod)
                        .currentBalance(0L)
                        .build())
                .build();
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    private String safeId(UUID value) {
        return value == null ? "" : value.toString();
    }

    private String buildChargeMetadata(ChargeRequest request) {
        return "{\"method\":\"CARD\",\"customerName\":\"" + safe(request.getCustomerName())
                + "\",\"cardLast4\":\"" + safe(request.getCardLast4())
                + "\",\"staffId\":\"" + safeId(request.getStaffId())
                + "\",\"staffName\":\"" + safe(request.getStaffName())
                + "\",\"storeId\":\"" + safeId(request.getStoreId()) + "\"}";
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
                .parentTransactionId(transaction.getParentTransactionId())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .failureReason(transaction.getFailureReason())
                .build();
    }
}
