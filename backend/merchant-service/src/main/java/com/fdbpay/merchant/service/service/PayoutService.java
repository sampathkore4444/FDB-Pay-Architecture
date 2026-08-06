package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.client.WalletServiceClient;
import com.fdbpay.merchant.service.dto.request.MerchantPreferencesRequest;
import com.fdbpay.merchant.service.dto.request.PayoutAccountRequest;
import com.fdbpay.merchant.service.dto.request.PayoutRequest;
import com.fdbpay.merchant.service.dto.response.ContractResponse;
import com.fdbpay.merchant.service.dto.response.MerchantPreferencesResponse;
import com.fdbpay.merchant.service.dto.response.PayoutAccountResponse;
import com.fdbpay.merchant.service.dto.response.PayoutResponse;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.Payout;
import com.fdbpay.merchant.service.model.PayoutAccount;
import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import com.fdbpay.merchant.service.model.enums.PayoutStatus;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.PayoutAccountRepository;
import com.fdbpay.merchant.service.repository.PayoutRepository;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutAccountRepository payoutAccountRepository;
    private final PayoutRepository payoutRepository;
    private final MerchantRepository merchantRepository;
    private final WalletServiceClient walletServiceClient;
    private final AuditService auditService;

    public List<PayoutAccountResponse> listAccounts(UUID merchantId) {
        requireMerchant(merchantId);
        return payoutAccountRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream().map(this::mapAccount).toList();
    }

    @Transactional
    public PayoutAccountResponse createAccount(UUID merchantId, PayoutAccountRequest request) {
        requireMerchant(merchantId);
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(merchantId);
        }
        boolean isFirst = payoutAccountRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).isEmpty();
        PayoutAccount account = PayoutAccount.builder()
                .merchantId(merchantId)
                .bankName(request.getBankName())
                .accountName(request.getAccountName())
                .accountNumber(request.getAccountNumber())
                .branch(request.getBranch())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()) || isFirst)
                .status(ActiveStatus.ACTIVE)
                .build();
        account = payoutAccountRepository.save(account);
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "PAYOUT_ACCOUNT",
                account.getId().toString(), "Added payout account " + account.getBankName() + " ending " + mask(account.getAccountNumber()));
        return mapAccount(account);
    }

    @Transactional
    public void deleteAccount(UUID merchantId, UUID accountId) {
        PayoutAccount account = getOwned(merchantId, accountId);
        payoutAccountRepository.delete(account);
        auditService.log(merchantId, "OWNER", null, null, "DELETE", "PAYOUT_ACCOUNT", accountId.toString(),
                "Removed payout account " + account.getBankName());
    }

    // ---- On-demand payouts ----

    @Transactional
    public PayoutResponse requestPayout(UUID merchantId, PayoutRequest request) {
        Merchant merchant = requireMerchant(merchantId);
        PayoutAccount account = getOwned(merchantId, request.getAccountId());
        Long available = walletServiceClient.getAvailableBalance(merchant.getUserId());
        if (available == null) {
            throw new BusinessException(ErrorCodes.SERVICE_UNAVAILABLE, "Unable to fetch wallet balance");
        }
        if (request.getAmount() > available) {
            throw new BusinessException(ErrorCodes.INSUFFICIENT_BALANCE,
                    "Insufficient balance. Available: " + available + " MMK");
        }
        String reference = "PO-" + System.currentTimeMillis();
        UUID walletId = walletServiceClient.getWalletId(merchant.getUserId());
        if (walletId == null) {
            throw new BusinessException(ErrorCodes.SERVICE_UNAVAILABLE, "Unable to resolve wallet");
        }
        boolean debited = walletServiceClient.debit(walletId, request.getAmount(),
                "On-demand payout to " + account.getBankName() + " ending " + mask(account.getAccountNumber()), UUID.randomUUID());
        Payout payout = Payout.builder()
                .merchantId(merchantId)
                .accountId(account.getId())
                .accountLabel(account.getBankName() + " ending " + mask(account.getAccountNumber()))
                .amount(request.getAmount())
                .status(PayoutStatus.PENDING)
                .reference(reference)
                .build();
        if (!debited) {
            payout.setStatus(PayoutStatus.FAILED);
            payout.setFailureReason("Wallet debit failed");
        }
        payout = payoutRepository.save(payout);
        if (debited) {
            payout.setStatus(PayoutStatus.COMPLETED);
            payout.setCompletedAt(OffsetDateTime.now());
            payout = payoutRepository.save(payout);
        }
        auditService.log(merchantId, "OWNER", null, null, "PAYOUT", "PAYOUT", payout.getId().toString(),
                "Requested payout of " + request.getAmount() + " MMK to " + payout.getAccountLabel() + " (" + payout.getStatus() + ")");
        return mapPayout(payout);
    }

    public Page<PayoutResponse> listPayouts(UUID merchantId, int page, int size) {
        requireMerchant(merchantId);
        return payoutRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(page, size))
                .map(this::mapPayout);
    }

    public Long getAvailableBalance(UUID merchantId) {
        Merchant merchant = requireMerchant(merchantId);
        return walletServiceClient.getAvailableBalance(merchant.getUserId());
    }

    public ContractResponse getContract(UUID merchantId) {
        Merchant merchant = requireMerchant(merchantId);
        ContractResponse.ContractResponseBuilder builder = ContractResponse.builder()
                .settlementType(merchant.getSettlementType())
                .settlementFrequencyDays(merchant.getRollingReservePeriodDays())
                .rollingReserveRate((long) merchant.getRollingReservePercent());
        try {
            double rate = merchant.getFeeSchedule() == null ? 0.02 : Double.parseDouble(merchant.getFeeSchedule());
            builder.feeRate(java.math.BigDecimal.valueOf(rate));
        } catch (Exception e) {
            builder.feeRate(java.math.BigDecimal.valueOf(0.02));
        }
        return builder.build();
    }

    @Transactional
    public PayoutAccountResponse setDefault(UUID merchantId, UUID accountId) {
        PayoutAccount account = getOwned(merchantId, accountId);
        clearDefault(merchantId);
        account.setIsDefault(true);
        account = payoutAccountRepository.save(account);
        auditService.log(merchantId, "OWNER", null, null, "UPDATE", "PAYOUT_ACCOUNT", accountId.toString(), "Set default payout account");
        return mapAccount(account);
    }

    public MerchantPreferencesResponse getPreferences(UUID merchantId) {
        Merchant merchant = requireMerchant(merchantId);
        return MerchantPreferencesResponse.builder()
                .settlementPreferredTime(merchant.getSettlementPreferredTime())
                .alertLargeOrderThreshold(merchant.getAlertLargeOrderThreshold())
                .alertDailySurgeThreshold(merchant.getAlertDailySurgeThreshold())
                .webhookUrl(merchant.getWebhookUrl())
                .build();
    }

    @Transactional
    public MerchantPreferencesResponse updatePreferences(UUID merchantId, MerchantPreferencesRequest request) {
        Merchant merchant = requireMerchant(merchantId);
        if (request.getSettlementPreferredTime() != null && !request.getSettlementPreferredTime().isBlank()) {
            merchant.setSettlementPreferredTime(request.getSettlementPreferredTime());
        }
        if (request.getAlertLargeOrderThreshold() != null) {
            merchant.setAlertLargeOrderThreshold(request.getAlertLargeOrderThreshold());
        }
        if (request.getAlertDailySurgeThreshold() != null) {
            merchant.setAlertDailySurgeThreshold(request.getAlertDailySurgeThreshold());
        }
        merchant.setWebhookUrl(request.getWebhookUrl());
        merchantRepository.save(merchant);
        auditService.log(merchantId, "OWNER", null, null, "UPDATE", "MERCHANT_PREFERENCES", merchantId.toString(),
                "Updated settlement/alerts/webhook preferences");
        return getPreferences(merchantId);
    }

    private void clearDefault(UUID merchantId) {
        for (PayoutAccount account : payoutAccountRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)) {
            if (Boolean.TRUE.equals(account.getIsDefault())) {
                account.setIsDefault(false);
                payoutAccountRepository.save(account);
            }
        }
    }

    private PayoutAccount getOwned(UUID merchantId, UUID accountId) {
        PayoutAccount account = payoutAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("PayoutAccount", accountId.toString()));
        if (!account.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Account does not belong to this merchant");
        }
        return account;
    }

    private Merchant requireMerchant(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private String mask(String number) {
        if (number == null || number.length() <= 4) {
            return number == null ? "" : number;
        }
        return "****" + number.substring(number.length() - 4);
    }

    private PayoutResponse mapPayout(Payout payout) {
        return PayoutResponse.builder()
                .id(payout.getId())
                .accountId(payout.getAccountId())
                .accountLabel(payout.getAccountLabel())
                .amount(payout.getAmount())
                .status(payout.getStatus())
                .reference(payout.getReference())
                .failureReason(payout.getFailureReason())
                .createdAt(payout.getCreatedAt())
                .completedAt(payout.getCompletedAt())
                .build();
    }

    private PayoutAccountResponse mapAccount(PayoutAccount account) {
        return PayoutAccountResponse.builder()
                .id(account.getId())
                .merchantId(account.getMerchantId())
                .bankName(account.getBankName())
                .accountName(account.getAccountName())
                .accountNumber(account.getAccountNumber())
                .branch(account.getBranch())
                .isDefault(account.getIsDefault())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
