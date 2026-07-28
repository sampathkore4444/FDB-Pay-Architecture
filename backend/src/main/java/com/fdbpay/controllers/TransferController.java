package com.fdbpay.controllers;

import com.fdbpay.schemas.request.TransferRequest;
import com.fdbpay.schemas.response.ApiResponse;
import com.fdbpay.schemas.response.TransactionResponse;
import com.fdbpay.services.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> initiateTransfer(
            @RequestParam UUID userId,
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transferService.initiateTransfer(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransferStatus(@PathVariable UUID id) {
        TransactionResponse response = transferService.getTransferStatus(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<TransactionResponse>> confirmTransfer(
            @PathVariable UUID id,
            @RequestParam String pin) {
        TransactionResponse response = transferService.confirmTransfer(id, pin);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelTransfer(@PathVariable UUID id) {
        transferService.cancelTransfer(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistory(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> history = transferService.getTransferHistory(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
