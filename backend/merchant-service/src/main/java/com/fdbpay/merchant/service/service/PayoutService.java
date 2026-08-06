package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.MerchantPreferencesRequest;
import com.fdbpay.merchant.service.dto.request.PayoutAccountRequest;
import com.fdbpay.merchant.service.dto.response.MerchantPreferencesResponse;
import com.fdbpay.merchant.service.dto.response.PayoutAccountResponse;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.PayoutAccount;
import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.PayoutAccountRepository;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutAccountRepository payoutAccountRepository;
    private final MerchantRepository merchantRepository;
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
