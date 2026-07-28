package com.fdbpay.wallet.service.service.impl;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.wallet.service.dto.request.CreateSavingsPocketRequest;
import com.fdbpay.wallet.service.dto.request.DepositToPocketRequest;
import com.fdbpay.wallet.service.dto.request.WithdrawFromPocketRequest;
import com.fdbpay.wallet.service.dto.response.SavingsPocketResponse;
import com.fdbpay.wallet.service.dto.response.SavingsTransactionResponse;
import com.fdbpay.wallet.service.model.SavingsPocket;
import com.fdbpay.wallet.service.model.SavingsTransaction;
import com.fdbpay.wallet.service.model.Wallet;
import com.fdbpay.wallet.service.model.enums.PocketStatus;
import com.fdbpay.wallet.service.model.enums.SavingsTxnType;
import com.fdbpay.wallet.service.model.enums.WalletStatus;
import com.fdbpay.wallet.service.repository.SavingsPocketRepository;
import com.fdbpay.wallet.service.repository.SavingsTransactionRepository;
import com.fdbpay.wallet.service.repository.WalletRepository;
import com.fdbpay.wallet.service.service.SavingsService;
import com.fdbpay.wallet.service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsServiceImpl implements SavingsService {

    private final SavingsPocketRepository savingsPocketRepository;
    private final SavingsTransactionRepository savingsTransactionRepository;
    private final WalletRepository walletRepository;
    @Lazy
    private final WalletService walletService;

    @Override
    @Transactional
    public ApiResponse<SavingsPocketResponse> createPocket(UUID walletId, CreateSavingsPocketRequest request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId.toString()));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.ACCOUNT_SUSPENDED, "Wallet is not active");
        }

        SavingsPocket pocket = SavingsPocket.builder()
                .id(UUID.randomUUID())
                .walletId(walletId)
                .name(request.getName())
                .goalAmount(request.getGoalAmount())
                .currentAmount(0L)
                .status(PocketStatus.ACTIVE)
                .targetDate(request.getTargetDate())
                .build();
        savingsPocketRepository.save(pocket);

        log.info("Savings pocket created: id={}, walletId={}, name={}, goal={}",
                pocket.getId(), walletId, request.getName(), request.getGoalAmount());

        return ApiResponse.success(mapToResponse(pocket));
    }

    @Override
    @Transactional
    public ApiResponse<SavingsPocketResponse> deposit(UUID pocketId, UUID walletId, DepositToPocketRequest request) {
        SavingsPocket pocket = savingsPocketRepository.findById(pocketId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings pocket", pocketId.toString()));

        if (!pocket.getWalletId().equals(walletId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Pocket does not belong to this wallet");
        }

        if (pocket.getStatus() != PocketStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Cannot deposit to a non-active pocket. Status: " + pocket.getStatus());
        }

        UUID txnId = UUID.randomUUID();
        walletService.debitWallet(walletId, request.getAmount(), "Savings deposit to pocket: " + pocket.getName(), txnId);

        pocket.setCurrentAmount(pocket.getCurrentAmount() + request.getAmount());
        savingsPocketRepository.save(pocket);

        SavingsTransaction transaction = SavingsTransaction.builder()
                .id(UUID.randomUUID())
                .pocketId(pocketId)
                .type(SavingsTxnType.DEPOSIT)
                .amount(request.getAmount())
                .balanceAfter(pocket.getCurrentAmount())
                .description("Deposit from wallet")
                .build();
        savingsTransactionRepository.save(transaction);

        log.info("Savings deposit: pocketId={}, walletId={}, amount={}, newBalance={}",
                pocketId, walletId, request.getAmount(), pocket.getCurrentAmount());

        return ApiResponse.success(mapToResponse(pocket));
    }

    @Override
    @Transactional
    public ApiResponse<SavingsPocketResponse> withdraw(UUID pocketId, UUID walletId, WithdrawFromPocketRequest request) {
        SavingsPocket pocket = savingsPocketRepository.findById(pocketId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings pocket", pocketId.toString()));

        if (!pocket.getWalletId().equals(walletId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Pocket does not belong to this wallet");
        }

        if (pocket.getStatus() != PocketStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Cannot withdraw from a non-active pocket. Status: " + pocket.getStatus());
        }

        if (pocket.getCurrentAmount() < request.getAmount()) {
            throw new com.fdbpay.shared.exceptions.InsufficientBalanceException(pocket.getCurrentAmount(), request.getAmount());
        }

        pocket.setCurrentAmount(pocket.getCurrentAmount() - request.getAmount());
        savingsPocketRepository.save(pocket);

        UUID txnId = UUID.randomUUID();
        walletService.creditWallet(walletId, request.getAmount(), "Savings withdrawal from pocket: " + pocket.getName(), txnId);

        SavingsTransaction transaction = SavingsTransaction.builder()
                .id(UUID.randomUUID())
                .pocketId(pocketId)
                .type(SavingsTxnType.WITHDRAWAL)
                .amount(request.getAmount())
                .balanceAfter(pocket.getCurrentAmount())
                .description("Withdrawal to wallet")
                .build();
        savingsTransactionRepository.save(transaction);

        log.info("Savings withdrawal: pocketId={}, walletId={}, amount={}, newBalance={}",
                pocketId, walletId, request.getAmount(), pocket.getCurrentAmount());

        return ApiResponse.success(mapToResponse(pocket));
    }

    @Override
    public ApiResponse<List<SavingsPocketResponse>> getPockets(UUID walletId) {
        List<SavingsPocket> pockets = savingsPocketRepository.findByWalletId(walletId);
        List<SavingsPocketResponse> responses = pockets.stream()
                .map(this::mapToResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    @Override
    public ApiResponse<SavingsPocketResponse> getPocket(UUID pocketId) {
        SavingsPocket pocket = savingsPocketRepository.findById(pocketId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings pocket", pocketId.toString()));
        return ApiResponse.success(mapToResponse(pocket));
    }

    @Override
    public ApiResponse<?> getTransactions(UUID pocketId, int page, int size) {
        savingsPocketRepository.findById(pocketId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings pocket", pocketId.toString()));

        Page<SavingsTransaction> transactions = savingsTransactionRepository
                .findByPocketIdOrderByCreatedAtDesc(pocketId, PageRequest.of(page, size));

        var response = transactions.map(this::mapToTransactionResponse);

        ApiResponse.Pagination pagination = ApiResponse.Pagination.builder()
                .page(response.getNumber())
                .perPage(response.getSize())
                .total(response.getTotalElements())
                .totalPages(response.getTotalPages())
                .build();

        ApiResponse.Meta meta = ApiResponse.Meta.builder()
                .requestId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now().toString())
                .pagination(pagination)
                .build();

        return ApiResponse.<Object>builder()
                .success(true)
                .data(response.getContent())
                .meta(meta)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<SavingsPocketResponse> pausePocket(UUID pocketId) {
        SavingsPocket pocket = savingsPocketRepository.findById(pocketId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings pocket", pocketId.toString()));

        if (pocket.getStatus() == PocketStatus.CLOSED) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Cannot pause a closed pocket");
        }

        pocket.setStatus(PocketStatus.PAUSED);
        savingsPocketRepository.save(pocket);

        log.info("Savings pocket paused: pocketId={}", pocketId);

        return ApiResponse.success(mapToResponse(pocket));
    }

    @Override
    @Transactional
    public ApiResponse<SavingsPocketResponse> closePocket(UUID pocketId) {
        SavingsPocket pocket = savingsPocketRepository.findById(pocketId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings pocket", pocketId.toString()));

        if (pocket.getStatus() == PocketStatus.CLOSED) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Pocket is already closed");
        }

        pocket.setStatus(PocketStatus.CLOSED);
        pocket.setClosedAt(OffsetDateTime.now());
        savingsPocketRepository.save(pocket);

        if (pocket.getCurrentAmount() > 0) {
            UUID txnId = UUID.randomUUID();
            walletService.creditWallet(pocket.getWalletId(), pocket.getCurrentAmount(),
                    "Savings pocket closed: " + pocket.getName(), txnId);

            SavingsTransaction transaction = SavingsTransaction.builder()
                    .id(UUID.randomUUID())
                    .pocketId(pocketId)
                    .type(SavingsTxnType.WITHDRAWAL)
                    .amount(pocket.getCurrentAmount())
                    .balanceAfter(0L)
                    .description("Pocket closure - funds returned to wallet")
                    .build();
            savingsTransactionRepository.save(transaction);

            pocket.setCurrentAmount(0L);
            savingsPocketRepository.save(pocket);
        }

        log.info("Savings pocket closed: pocketId={}", pocketId);

        return ApiResponse.success(mapToResponse(pocket));
    }

    @Override
    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void calculateInterest() {
        List<SavingsPocket> activePockets = savingsPocketRepository.findByStatus(PocketStatus.ACTIVE);

        for (SavingsPocket pocket : activePockets) {
            if (pocket.getCurrentAmount() <= 0) {
                continue;
            }

            BigDecimal monthlyRate = pocket.getInterestRate().divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
            Long interestAmount = pocket.getCurrentAmount()
                    .multiply(monthlyRate)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            if (interestAmount <= 0) {
                continue;
            }

            pocket.setCurrentAmount(pocket.getCurrentAmount() + interestAmount);
            savingsPocketRepository.save(pocket);

            SavingsTransaction transaction = SavingsTransaction.builder()
                    .id(UUID.randomUUID())
                    .pocketId(pocket.getId())
                    .type(SavingsTxnType.INTEREST)
                    .amount(interestAmount)
                    .balanceAfter(pocket.getCurrentAmount())
                    .description("Monthly interest accrual")
                    .build();
            savingsTransactionRepository.save(transaction);

            log.info("Interest credited: pocketId={}, amount={}, newBalance={}",
                    pocket.getId(), interestAmount, pocket.getCurrentAmount());
        }
    }

    private SavingsPocketResponse mapToResponse(SavingsPocket pocket) {
        double progress = 0.0;
        if (pocket.getGoalAmount() > 0) {
            progress = ((double) pocket.getCurrentAmount() / pocket.getGoalAmount()) * 100.0;
            progress = Math.min(progress, 100.0);
        }

        return SavingsPocketResponse.builder()
                .id(pocket.getId())
                .name(pocket.getName())
                .goalAmount(pocket.getGoalAmount())
                .currentAmount(pocket.getCurrentAmount())
                .progressPercentage(progress)
                .interestRate(pocket.getInterestRate())
                .status(pocket.getStatus())
                .targetDate(pocket.getTargetDate())
                .createdAt(pocket.getCreatedAt())
                .build();
    }

    private SavingsTransactionResponse mapToTransactionResponse(SavingsTransaction transaction) {
        return SavingsTransactionResponse.builder()
                .id(transaction.getId())
                .pocketId(transaction.getPocketId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
