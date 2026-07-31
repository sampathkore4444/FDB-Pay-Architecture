package com.fdbpay.wallet.service.service;

import com.fdbpay.wallet.service.dto.request.TopUpRequest;
import com.fdbpay.wallet.service.dto.response.LedgerEntryResponse;
import com.fdbpay.wallet.service.dto.response.WalletResponse;
import com.fdbpay.wallet.service.model.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface WalletService {

    WalletResponse getWallet(UUID userId);

    WalletResponse createWallet(UUID userId);

    WalletResponse topUp(UUID userId, TopUpRequest request);

    WalletResponse withdraw(UUID userId, Long amount, String idempotencyKey);

    Page<LedgerEntryResponse> getLedger(UUID userId, Pageable pageable);

    Map<String, Long> getLimits(UUID userId);

    void debitWallet(UUID walletId, Long amount, String description, UUID txnId);

    void creditWallet(UUID walletId, Long amount, String description, UUID txnId);

    UUID getWalletOwner(UUID walletId);
}
