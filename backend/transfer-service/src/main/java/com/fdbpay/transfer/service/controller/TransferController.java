package com.fdbpay.transfer.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.transfer.service.dto.request.TransferRequest;
import com.fdbpay.transfer.service.dto.response.TransactionResponse;
import com.fdbpay.transfer.service.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @GetMapping
    public ApiResponse<Page<TransactionResponse>> getTransfers(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(transferService.getHistory(userId, page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> initiateTransfer(
            @Valid @RequestBody TransferRequest request) {
        return ApiResponse.success(transferService.initiateTransfer(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> getTransferStatus(@PathVariable UUID id) {
        return ApiResponse.success(transferService.getTransferStatus(id));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<TransactionResponse> confirmTransfer(@PathVariable UUID id) {
        return ApiResponse.success(transferService.confirmTransfer(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<TransactionResponse> cancelTransfer(@PathVariable UUID id) {
        return ApiResponse.success(transferService.cancelTransfer(id));
    }

    @GetMapping("/history")
    public ApiResponse<Page<TransactionResponse>> getHistory(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(transferService.getHistory(userId, page, size));
    }
}
