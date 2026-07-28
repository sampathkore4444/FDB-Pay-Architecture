package com.fdbpay.merchant.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.merchant.service.dto.request.CreateInvoiceRequest;
import com.fdbpay.merchant.service.dto.response.InvoiceResponse;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/merchant/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final MerchantRepository merchantRepository;

    private UUID resolveMerchantId(UUID userId) {
        return merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId=" + userId)).getId();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvoiceResponse> create(
            @RequestParam UUID userId,
            @Valid @RequestBody CreateInvoiceRequest request) {
        return ApiResponse.success(invoiceService.create(resolveMerchantId(userId), request));
    }

    @PutMapping("/{id}/send")
    public ApiResponse<InvoiceResponse> send(
            @RequestParam UUID userId,
            @PathVariable UUID id) {
        return ApiResponse.success(invoiceService.send(id));
    }

    @PutMapping("/{id}/paid")
    public ApiResponse<InvoiceResponse> markPaid(
            @RequestParam UUID userId,
            @PathVariable UUID id) {
        return ApiResponse.success(invoiceService.markPaid(id));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<InvoiceResponse> cancel(
            @RequestParam UUID userId,
            @PathVariable UUID id) {
        return ApiResponse.success(invoiceService.cancel(id));
    }

    @GetMapping
    public ApiResponse<Page<InvoiceResponse>> getByMerchant(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(invoiceService.getByMerchant(resolveMerchantId(userId), page, size));
    }
}
