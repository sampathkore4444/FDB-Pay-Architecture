package com.fdbpay.wallet.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.wallet.service.dto.request.CreateSavingsPocketRequest;
import com.fdbpay.wallet.service.dto.request.DepositToPocketRequest;
import com.fdbpay.wallet.service.dto.request.WithdrawFromPocketRequest;
import com.fdbpay.wallet.service.model.enums.WalletStatus;
import com.fdbpay.wallet.service.repository.WalletRepository;
import com.fdbpay.wallet.service.service.SavingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/wallet/savings")
@RequiredArgsConstructor
public class SavingsController {

    private final SavingsService savingsService;
    private final WalletRepository walletRepository;

    private UUID resolveWalletId(UUID userId) {
        return walletRepository.findActiveWalletByUserIdAndStatus(userId, WalletStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId=" + userId)).getId();
    }

    @PostMapping("/pockets")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> createPocket(
            @RequestParam UUID userId,
            @Valid @RequestBody CreateSavingsPocketRequest request) {
        return savingsService.createPocket(resolveWalletId(userId), request);
    }

    @GetMapping("/pockets")
    public ApiResponse<?> getPockets(@RequestParam UUID userId) {
        return savingsService.getPockets(resolveWalletId(userId));
    }

    @GetMapping("/pockets/{id}")
    public ApiResponse<?> getPocket(@PathVariable UUID id) {
        return savingsService.getPocket(id);
    }

    @PostMapping("/pockets/{id}/deposit")
    public ApiResponse<?> deposit(
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @Valid @RequestBody DepositToPocketRequest request) {
        return savingsService.deposit(id, resolveWalletId(userId), request);
    }

    @PostMapping("/pockets/{id}/withdraw")
    public ApiResponse<?> withdraw(
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @Valid @RequestBody WithdrawFromPocketRequest request) {
        return savingsService.withdraw(id, resolveWalletId(userId), request);
    }

    @PutMapping("/pockets/{id}/pause")
    public ApiResponse<?> pausePocket(@PathVariable UUID id) {
        return savingsService.pausePocket(id);
    }

    @PutMapping("/pockets/{id}/close")
    public ApiResponse<?> closePocket(@PathVariable UUID id) {
        return savingsService.closePocket(id);
    }

    @GetMapping("/pockets/{id}/transactions")
    public ApiResponse<?> getTransactions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return savingsService.getTransactions(id, page, size);
    }
}
