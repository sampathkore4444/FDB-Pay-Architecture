package com.fdbpay.merchant.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.merchant.service.dto.request.MerchantRegisterRequest;
import com.fdbpay.merchant.service.dto.response.MerchantResponse;
import com.fdbpay.merchant.service.dto.response.QrCodeResponse;
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

    @PutMapping("/{id}")
    public ApiResponse<MerchantResponse> updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody MerchantRegisterRequest request) {
        return ApiResponse.success(merchantService.updateProfile(id, request));
    }

    @GetMapping("/{id}/qr")
    public ApiResponse<QrCodeResponse> generateQrCode(@PathVariable UUID id) {
        return ApiResponse.success(merchantService.generateQrCode(id));
    }
}
