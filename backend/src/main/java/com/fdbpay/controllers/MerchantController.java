package com.fdbpay.controllers;

import com.fdbpay.schemas.request.MerchantRegisterRequest;
import com.fdbpay.schemas.response.ApiResponse;
import com.fdbpay.schemas.response.MerchantResponse;
import com.fdbpay.services.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<MerchantResponse>> register(
            @RequestParam UUID userId,
            @Valid @RequestBody MerchantRegisterRequest request) {
        MerchantResponse response = merchantService.register(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<MerchantResponse>> getProfile(@RequestParam UUID merchantId) {
        MerchantResponse response = merchantService.getProfile(merchantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<MerchantResponse>> updateProfile(
            @RequestParam UUID merchantId,
            @Valid @RequestBody MerchantRegisterRequest request) {
        MerchantResponse response = merchantService.updateProfile(merchantId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTransactions(
            @RequestParam UUID merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> transactions = merchantService.getTransactions(merchantId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/settlements")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettlements(
            @RequestParam UUID merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> settlements = merchantService.getSettlements(merchantId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(settlements));
    }

    @GetMapping("/settlements/{settlementId}/detail")
    public ResponseEntity<ApiResponse<com.fdbpay.schemas.response.SettlementResponse>> getSettlementDetail(
            @RequestParam UUID merchantId,
            @PathVariable UUID settlementId) {
        var response = merchantService.getSettlementDetail(merchantId, settlementId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/qr/generate")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateQr(
            @RequestParam UUID merchantId,
            @RequestParam(defaultValue = "static") String type,
            @RequestParam(required = false) Long amount) {
        String qrData = merchantService.generateQrCode(merchantId, type, amount);
        return ResponseEntity.ok(ApiResponse.success(Map.of("qrData", qrData)));
    }
}
