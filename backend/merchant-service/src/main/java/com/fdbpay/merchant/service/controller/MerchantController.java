package com.fdbpay.merchant.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.merchant.service.dto.request.MerchantRegisterRequest;
import com.fdbpay.merchant.service.dto.response.MerchantResponse;
import com.fdbpay.merchant.service.dto.response.QrCodeResponse;
import com.fdbpay.merchant.service.model.enums.SettlementType;
import com.fdbpay.merchant.service.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MerchantResponse> register(
            @RequestParam UUID userId,
            @Valid @RequestBody MerchantRegisterRequest request) {
        return ApiResponse.success(merchantService.register(userId, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<MerchantResponse> getProfile(@PathVariable UUID id) {
        return ApiResponse.success(merchantService.getProfile(id));
    }

    @GetMapping("/by-user/{userId}")
    public ApiResponse<MerchantResponse> getProfileByUserId(@PathVariable UUID userId) {
        return ApiResponse.success(merchantService.getProfileByUserId(userId));
    }

    @PutMapping("/{id}")
    public ApiResponse<MerchantResponse> updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody MerchantRegisterRequest request) {
        return ApiResponse.success(merchantService.updateProfile(id, request));
    }

    @GetMapping("/{id}/qr")
    public ApiResponse<QrCodeResponse> generateQrCode(
            @PathVariable UUID id,
            @RequestParam(required = false) Long amount) {
        return ApiResponse.success(merchantService.generateQrCode(id, amount));
    }

    @PutMapping("/{id}/settlement-type")
    public ApiResponse<MerchantResponse> updateSettlementType(
            @PathVariable UUID id,
            @RequestParam SettlementType settlementType) {
        return ApiResponse.success(merchantService.updateSettlementType(id, settlementType));
    }

    @PutMapping("/{id}/terminal-fields")
    public ApiResponse<MerchantResponse> updateTerminalFields(
            @PathVariable UUID id,
            @RequestBody String terminalFields) {
        return ApiResponse.success(merchantService.updateTerminalFields(id, terminalFields));
    }

    @PutMapping("/{id}/reserve")
    public ApiResponse<MerchantResponse> updateRollingReserve(
            @PathVariable UUID id,
            @RequestParam Integer percent,
            @RequestParam(defaultValue = "7") Integer periodDays) {
        return ApiResponse.success(merchantService.updateRollingReserve(id, percent, periodDays));
    }
}
