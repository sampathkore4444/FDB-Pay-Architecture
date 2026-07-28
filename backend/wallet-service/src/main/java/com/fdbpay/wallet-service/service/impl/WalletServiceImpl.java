package com.fdbpay.wallet.service.service.impl;

import com.fdbpay.shared.constants.AppConstants;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.event.TransactionEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.InsufficientBalanceException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.wallet.service.dto.request.TopUpRequest;
import com.fdbpay.wallet.service.dto.response.LedgerEntryResponse;
import com.fdbpay.wallet.service.dto.response.WalletResponse;
import com.fdbpay.wallet.service.model.LedgerEntry;
import com.fdbpay.wallet.service.model.Wallet;
import com.fdbpay.wallet.service.model.enums.LedgerEntryType;
import com.fdbpay.wallet.service.model.enums.WalletStatus;
import com.fdbpay.wallet.service.repository.LedgerEntryRepository;
import com.fdbpay.wallet.service.repository.WalletRepository;
import com.fdbpay.wallet.service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Cacheable(value = "wallet", key = "#userId")
    public WalletResponse getWallet(UUID userId) {
        Wallet wallet = getActiveWalletByUserId(userId);
        return mapToResponse(wallet);
    }

    @Override
    @Transactional
    @CacheEvict(value = "wallet", key = "#userId")
    public WalletResponse topUp(UUID userId, TopUpRequest request) {
        Wallet wallet = getActiveWalletByUserId(userId);

        checkIdempotency(wallet.getId(), request.getIdempotencyKey());

        wallet.setBalanceTotal(wallet.getBalanceTotal() + request.getAmount());
        walletRepository.save(wallet);

        UUID txnId = UUID.randomUUID();
        createLedgerEntry(wallet.getId(), LedgerEntryType.CREDIT, request.getAmount(),
                wallet.getBalanceTotal(), txnId, "Top-up via " + request.getChannel());

        publishEvent(txnId, "TOPUP", "COMPLETED", wallet.getId(), null,
                request.getAmount(), wallet.getCurrency(), userId, null,
                Map.of("channel", request.getChannel() != null ? request.getChannel() : "direct"));

        invalidateCache(userId);
        log.info("Top-up completed: userId={}, amount={}, newBalance={}", userId, request.getAmount(), wallet.getBalanceTotal());

        return mapToResponse(wallet);
    }

    @Override
    @Transactional
    @CacheEvict(value = "wallet", key = "#userId")
    public WalletResponse withdraw(UUID userId, Long amount, String idempotencyKey) {
        Wallet wallet = getActiveWalletByUserId(userId);

        checkIdempotency(wallet.getId(), idempotencyKey);

        Long available = wallet.getAvailableBalance();
        if (available < amount) {
            throw new InsufficientBalanceException(available, amount);
        }

        wallet.setBalanceTotal(wallet.getBalanceTotal() - amount);
        walletRepository.save(wallet);

        UUID txnId = UUID.randomUUID();
        createLedgerEntry(wallet.getId(), LedgerEntryType.DEBIT, amount,
                wallet.getBalanceTotal(), txnId, "Withdrawal");

        publishEvent(txnId, "WITHDRAWAL", "COMPLETED", null, wallet.getId(),
                amount, wallet.getCurrency(), null, userId, Map.of());

        invalidateCache(userId);
        log.info("Withdrawal completed: userId={}, amount={}, newBalance={}", userId, amount, wallet.getBalanceTotal());

        return mapToResponse(wallet);
    }

    @Override
    public Page<LedgerEntryResponse> getLedger(UUID userId, Pageable pageable) {
        Wallet wallet = getActiveWalletByUserId(userId);
        Page<LedgerEntry> entries = ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable);
        return entries.map(this::mapToLedgerResponse);
    }

    @Override
    public Map<String, Long> getLimits(UUID userId) {
        Wallet wallet = getActiveWalletByUserId(userId);
        return Map.of(
                "dailyLimit", wallet.getDailyLimit(),
                "monthlyLimit", wallet.getMonthlyLimit()
        );
    }

    @Override
    @Transactional
    public void debitWallet(UUID walletId, Long amount, String description, UUID txnId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId.toString()));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.ACCOUNT_SUSPENDED,
                    "Wallet is not active. Status: " + wallet.getStatus());
        }

        Long available = wallet.getAvailableBalance();
        if (available < amount) {
            throw new InsufficientBalanceException(available, amount);
        }

        wallet.setBalanceTotal(wallet.getBalanceTotal() - amount);
        walletRepository.save(wallet);

        createLedgerEntry(walletId, LedgerEntryType.DEBIT, amount,
                wallet.getBalanceTotal(), txnId, description);

        invalidateCache(wallet.getUserId());
        log.info("Debit completed: walletId={}, amount={}, txnId={}", walletId, amount, txnId);
    }

    @Override
    @Transactional
    public void creditWallet(UUID walletId, Long amount, String description, UUID txnId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId.toString()));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.ACCOUNT_SUSPENDED,
                    "Wallet is not active. Status: " + wallet.getStatus());
        }

        wallet.setBalanceTotal(wallet.getBalanceTotal() + amount);
        walletRepository.save(wallet);

        createLedgerEntry(walletId, LedgerEntryType.CREDIT, amount,
                wallet.getBalanceTotal(), txnId, description);

        invalidateCache(wallet.getUserId());
        log.info("Credit completed: walletId={}, amount={}, txnId={}", walletId, amount, txnId);
    }

    private Wallet getActiveWalletByUserId(UUID userId) {
        return walletRepository.findActiveWalletByUserIdAndStatus(userId, WalletStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId.toString()));
    }

    private void checkIdempotency(UUID walletId, String idempotencyKey) {
        String cacheKey = AppConstants.IDEMPOTENCY_CACHE_PREFIX + walletId + ":" + idempotencyKey;
        Boolean exists = redisTemplate.hasKey(cacheKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BusinessException(ErrorCodes.DUPLICATE_TRANSACTION,
                    "Duplicate transaction detected for idempotency key: " + idempotencyKey);
        }
        redisTemplate.opsForValue().set(cacheKey, "1", 24, TimeUnit.HOURS);
    }

    private void createLedgerEntry(UUID walletId, LedgerEntryType type, Long amount,
                                    Long balanceAfter, UUID txnId, String description) {
        LedgerEntry entry = LedgerEntry.builder()
                .walletId(walletId)
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .txnId(txnId)
                .description(description)
                .build();
        ledgerEntryRepository.save(entry);
    }

    private void publishEvent(UUID txnId, String type, String status,
                               UUID senderWalletId, UUID receiverWalletId,
                               Long amount, String currency,
                               UUID senderUserId, UUID receiverUserId,
                               Map<String, Object> metadata) {
        try {
            TransactionEvent event = TransactionEvent.builder()
                    .transactionId(txnId)
                    .type(type)
                    .status(status)
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(receiverWalletId)
                    .amount(amount)
                    .currency(currency)
                    .senderUserId(senderUserId)
                    .receiverUserId(receiverUserId)
                    .timestamp(OffsetDateTime.now())
                    .metadata(metadata)
                    .build();
            kafkaTemplate.send(AppConstants.TOPIC_TXN_COMPLETED, txnId.toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish transaction event: txnId={}", txnId, e);
        }
    }

    private void invalidateCache(UUID userId) {
        redisTemplate.delete("wallet::" + userId);
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus())
                .balanceTotal(wallet.getBalanceTotal())
                .balanceAvailable(wallet.getAvailableBalance())
                .balanceHeld(wallet.getBalanceHeld())
                .balanceFrozen(wallet.getBalanceFrozen())
                .dailyLimit(wallet.getDailyLimit())
                .monthlyLimit(wallet.getMonthlyLimit())
                .kycTier(wallet.getKycTier())
                .createdAt(wallet.getCreatedAt())
                .build();
    }

    private LedgerEntryResponse mapToLedgerResponse(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
                .id(entry.getId())
                .type(entry.getType())
                .amount(entry.getAmount())
                .balanceAfter(entry.getBalanceAfter())
                .txnId(entry.getTxnId())
                .description(entry.getDescription())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
