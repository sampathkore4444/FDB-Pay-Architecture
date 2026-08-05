package com.fdbpay.merchant.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.merchant.service.dto.request.CreatePaymentLinkRequest;
import com.fdbpay.merchant.service.dto.response.PaymentLinkResponse;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.service.PaymentLinkService;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/merchant/payment-links")
@RequiredArgsConstructor
public class MerchantPaymentLinkController {

    private final PaymentLinkService paymentLinkService;
    private final MerchantRepository merchantRepository;

    private UUID resolveMerchantId(UUID userId) {
        return merchantRepository.findByUserId(userId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId=" + userId)).getId();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentLinkResponse> create(
            @RequestParam UUID userId,
            @Valid @RequestBody CreatePaymentLinkRequest request) {
        return ApiResponse.success(paymentLinkService.create(resolveMerchantId(userId), request));
    }

    @GetMapping
    public ApiResponse<Page<PaymentLinkResponse>> getByMerchant(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(paymentLinkService.getByMerchant(resolveMerchantId(userId), page, size));
    }

    @PutMapping("/{id}/deactivate")
    public ApiResponse<PaymentLinkResponse> deactivate(
            @RequestParam UUID userId,
            @PathVariable UUID id) {
        return ApiResponse.success(paymentLinkService.deactivate(resolveMerchantId(userId), id));
    }
}
