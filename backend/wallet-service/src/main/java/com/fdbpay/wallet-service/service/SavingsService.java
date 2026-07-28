package com.fdbpay.wallet.service.service;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.wallet.service.dto.request.CreateSavingsPocketRequest;
import com.fdbpay.wallet.service.dto.request.DepositToPocketRequest;
import com.fdbpay.wallet.service.dto.request.WithdrawFromPocketRequest;
import com.fdbpay.wallet.service.dto.response.SavingsPocketResponse;
import com.fdbpay.wallet.service.dto.response.SavingsTransactionResponse;

import java.util.List;
import java.util.UUID;

public interface SavingsService {

    ApiResponse<SavingsPocketResponse> createPocket(UUID walletId, CreateSavingsPocketRequest request);

    ApiResponse<SavingsPocketResponse> deposit(UUID pocketId, UUID walletId, DepositToPocketRequest request);

    ApiResponse<SavingsPocketResponse> withdraw(UUID pocketId, UUID walletId, WithdrawFromPocketRequest request);

    ApiResponse<List<SavingsPocketResponse>> getPockets(UUID walletId);

    ApiResponse<SavingsPocketResponse> getPocket(UUID pocketId);

    ApiResponse<?> getTransactions(UUID pocketId, int page, int size);

    ApiResponse<SavingsPocketResponse> pausePocket(UUID pocketId);

    ApiResponse<SavingsPocketResponse> closePocket(UUID pocketId);

    void calculateInterest();
}
