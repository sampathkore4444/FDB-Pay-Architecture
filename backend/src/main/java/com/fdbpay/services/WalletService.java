package com.fdbpay.services;

import com.fdbpay.schemas.request.TopUpRequest;
import com.fdbpay.schemas.response.WalletResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface WalletService {

    WalletResponse getWallet(UUID userId);

    WalletResponse topUp(UUID userId, TopUpRequest request);

    WalletResponse withdraw(UUID userId, Long amount, String idempotencyKey);

    Map<String, Object> getLedger(UUID userId, Pageable pageable);

    Map<String, Object> getLimits(UUID userId);

    void debitWallet(UUID walletId, Long amount, String description, UUID txnId);

    void creditWallet(UUID walletId, Long amount, String description, UUID txnId);
}
