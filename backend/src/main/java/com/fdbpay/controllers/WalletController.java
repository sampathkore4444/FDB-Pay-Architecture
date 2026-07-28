package com.fdbpay.controllers;

import com.fdbpay.schemas.request.TopUpRequest;
import com.fdbpay.schemas.response.ApiResponse;
import com.fdbpay.schemas.response.WalletResponse;
import com.fdbpay.services.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@RequestParam UUID userId) {
        WalletResponse wallet = walletService.getWallet(userId);
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }

    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLedger(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> ledger = walletService.getLedger(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(ledger));
    }

    @GetMapping("/limits")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLimits(@RequestParam UUID userId) {
        Map<String, Object> limits = walletService.getLimits(userId);
        return ResponseEntity.ok(ApiResponse.success(limits));
    }

    @PostMapping("/topup")
    public ResponseEntity<ApiResponse<WalletResponse>> topUp(
            @RequestParam UUID userId,
            @Valid @RequestBody TopUpRequest request) {
        WalletResponse wallet = walletService.topUp(userId, request);
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WalletResponse>> withdraw(
            @RequestParam UUID userId,
            @RequestParam Long amount,
            @RequestParam String idempotencyKey) {
        WalletResponse wallet = walletService.withdraw(userId, amount, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }
}
