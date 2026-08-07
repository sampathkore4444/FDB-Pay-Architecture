package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.CreateTaxInvoiceRequest;
import com.fdbpay.merchant.service.dto.response.TaxInvoiceResponse;
import com.fdbpay.merchant.service.dto.response.TaxSummaryResponse;
import com.fdbpay.merchant.service.service.TaxService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping("/tax-invoices")
    public ApiResponse<List<TaxInvoiceResponse>> listInvoices(@RequestParam UUID userId) {
        return ApiResponse.success(taxService.listInvoices(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping("/tax-invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TaxInvoiceResponse> createInvoice(@RequestParam UUID userId,
                                                         @Valid @RequestBody CreateTaxInvoiceRequest request) {
        return ApiResponse.success(taxService.createInvoice(accessHelper.resolveMerchantId(userId), request));
    }

    @GetMapping("/tax/summary")
    public ApiResponse<TaxSummaryResponse> summary(@RequestParam UUID userId,
                                                   @RequestParam(required = false) LocalDate from,
                                                   @RequestParam(required = false) LocalDate to) {
        return ApiResponse.success(taxService.summary(accessHelper.resolveMerchantId(userId), from, to));
    }
}
