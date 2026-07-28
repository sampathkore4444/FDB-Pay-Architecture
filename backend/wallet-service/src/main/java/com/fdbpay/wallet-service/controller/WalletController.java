package com.fdbpay.wallet.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.wallet.service.dto.request.TopUpRequest;
import com.fdbpay.wallet.service.dto.request.WithdrawRequest;
import com.fdbpay.wallet.service.dto.response.LedgerEntryResponse;
import com.fdbpay.wallet.service.dto.response.WalletResponse;
import com.fdbpay.wallet.service.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ApiResponse<WalletResponse> getWallet(@RequestParam UUID userId) {
        return ApiResponse.success(walletService.getWallet(userId));
    }

    @GetMapping("/ledger")
    public ApiResponse<Page<LedgerEntryResponse>> getLedger(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(walletService.getLedger(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/limits")
    public ApiResponse<Map<String, Long>> getLimits(@RequestParam UUID userId) {
        return ApiResponse.success(walletService.getLimits(userId));
    }

    @PostMapping("/topup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WalletResponse> topUp(
            @RequestParam UUID userId,
            @Valid @RequestBody TopUpRequest request) {
        return ApiResponse.success(walletService.topUp(userId, request));
    }

    @PostMapping("/withdraw")
    public ApiResponse<WalletResponse> withdraw(
            @RequestParam UUID userId,
            @Valid @RequestBody WithdrawRequest request) {
        return ApiResponse.success(walletService.withdraw(userId, request.getAmount(), request.getIdempotencyKey()));
    }
}
