package com.fdbpay.services.impl;

import com.fdbpay.common.constants.AppConstants;
import com.fdbpay.common.exceptions.BusinessException;
import com.fdbpay.common.constants.ErrorCodes;
import com.fdbpay.common.exceptions.InsufficientBalanceException;
import com.fdbpay.common.exceptions.ResourceNotFoundException;
import com.fdbpay.models.entity.LedgerEntry;
import com.fdbpay.models.entity.User;
import com.fdbpay.models.entity.Wallet;
import com.fdbpay.models.enums.LedgerEntryType;
import com.fdbpay.models.enums.WalletStatus;
import com.fdbpay.repositories.LedgerEntryRepository;
import com.fdbpay.repositories.UserRepository;
import com.fdbpay.repositories.WalletRepository;
import com.fdbpay.schemas.request.TopUpRequest;
import com.fdbpay.schemas.response.WalletResponse;
import com.fdbpay.services.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public WalletResponse getWallet(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId.toString()));
        return mapToResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse topUp(UUID userId, TopUpRequest request) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId.toString()));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.ACCOUNT_SUSPENDED, "Wallet is not active");
        }

        wallet.setBalanceTotal(wallet.getBalanceTotal() + request.getAmount());
        walletRepository.save(wallet);

        LedgerEntry entry = LedgerEntry.builder()
                .wallet(wallet)
                .type(LedgerEntryType.CREDIT)
                .amount(request.getAmount())
                .balanceAfter(wallet.getBalanceTotal())
                .txnId(UUID.randomUUID())
                .description("Top-up via " + request.getChannel())
                .build();
        ledgerEntryRepository.save(entry);

        invalidateBalanceCache(wallet.getId());

        log.info("Wallet top-up: userId={}, amount={}, channel={}", userId, request.getAmount(), request.getChannel());
        return mapToResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse withdraw(UUID userId, Long amount, String idempotencyKey) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId.toString()));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.ACCOUNT_SUSPENDED, "Wallet is not active");
        }

        Long available = wallet.getAvailableBalance();
        if (available < amount) {
            throw new InsufficientBalanceException(available, amount);
        }

        wallet.setBalanceTotal(wallet.getBalanceTotal() - amount);
        walletRepository.save(wallet);

        LedgerEntry entry = LedgerEntry.builder()
                .wallet(wallet)
                .type(LedgerEntryType.DEBIT)
                .amount(amount)
                .balanceAfter(wallet.getBalanceTotal())
                .txnId(UUID.randomUUID())
                .description("Withdrawal")
                .build();
        ledgerEntryRepository.save(entry);

        invalidateBalanceCache(wallet.getId());

        return mapToResponse(wallet);
    }

    @Override
    public Map<String, Object> getLedger(UUID userId, Pageable pageable) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId.toString()));

        Page<LedgerEntry> entries = ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("entries", entries.getContent());
        result.put("page", entries.getNumber());
        result.put("size", entries.getSize());
        result.put("total", entries.getTotalElements());
        result.put("totalPages", entries.getTotalPages());
        return result;
    }

    @Override
    public Map<String, Object> getLimits(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId.toString()));

        Map<String, Object> limits = new HashMap<>();
        limits.put("dailyLimit", wallet.getDailyLimit());
        limits.put("monthlyLimit", wallet.getMonthlyLimit());
        limits.put("kycTier", wallet.getKycTier().name());
        return limits;
    }

    @Override
    @Transactional
    public void debitWallet(UUID walletId, Long amount, String description, UUID txnId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId.toString()));

        Long available = wallet.getAvailableBalance();
        if (available < amount) {
            throw new InsufficientBalanceException(available, amount);
        }

        wallet.setBalanceTotal(wallet.getBalanceTotal() - amount);
        walletRepository.save(wallet);

        LedgerEntry entry = LedgerEntry.builder()
                .wallet(wallet)
                .type(LedgerEntryType.DEBIT)
                .amount(amount)
                .balanceAfter(wallet.getBalanceTotal())
                .txnId(txnId)
                .description(description)
                .build();
        ledgerEntryRepository.save(entry);

        invalidateBalanceCache(walletId);
    }

    @Override
    @Transactional
    public void creditWallet(UUID walletId, Long amount, String description, UUID txnId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId.toString()));

        wallet.setBalanceTotal(wallet.getBalanceTotal() + amount);
        walletRepository.save(wallet);

        LedgerEntry entry = LedgerEntry.builder()
                .wallet(wallet)
                .type(LedgerEntryType.CREDIT)
                .amount(amount)
                .balanceAfter(wallet.getBalanceTotal())
                .txnId(txnId)
                .description(description)
                .build();
        ledgerEntryRepository.save(entry);

        invalidateBalanceCache(walletId);
    }

    private void invalidateBalanceCache(UUID walletId) {
        redisTemplate.delete(AppConstants.WALLET_BALANCE_CACHE_PREFIX + walletId);
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().name())
                .balanceTotal(wallet.getBalanceTotal())
                .balanceAvailable(wallet.getAvailableBalance())
                .balanceHeld(wallet.getBalanceHeld())
                .balanceFrozen(wallet.getBalanceFrozen())
                .dailyLimit(wallet.getDailyLimit())
                .monthlyLimit(wallet.getMonthlyLimit())
                .kycTier(wallet.getKycTier().name())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}
