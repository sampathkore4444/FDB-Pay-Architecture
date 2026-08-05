package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.AddChargebackNoteRequest;
import com.fdbpay.merchant.service.dto.request.AddChargebackRequest;
import com.fdbpay.merchant.service.dto.request.RespondChargebackRequest;
import com.fdbpay.merchant.service.dto.response.ChargebackResponse;
import com.fdbpay.merchant.service.service.ChargebackService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/chargebacks")
@RequiredArgsConstructor
public class ChargebackController {

    private final ChargebackService chargebackService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping
    public ApiResponse<List<ChargebackResponse>> getChargebacks(@RequestParam UUID userId) {
        return ApiResponse.success(chargebackService.getByMerchant(accessHelper.resolveMerchantId(userId)));
    }

    @GetMapping("/{chargebackId}")
    public ApiResponse<ChargebackResponse> getDetail(
            @RequestParam UUID userId,
            @PathVariable UUID chargebackId) {
        return ApiResponse.success(chargebackService.getDetail(accessHelper.resolveMerchantId(userId), chargebackId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChargebackResponse> openChargeback(
            @RequestParam UUID userId,
            @Valid @RequestBody AddChargebackRequest request) {
        return ApiResponse.success(chargebackService.openChargeback(accessHelper.resolveMerchantId(userId), request));
    }

    @PostMapping("/{chargebackId}/notes")
    public ApiResponse<ChargebackResponse> addNote(
            @RequestParam UUID userId,
            @PathVariable UUID chargebackId,
            @Valid @RequestBody AddChargebackNoteRequest request) {
        return ApiResponse.success(chargebackService.addNote(accessHelper.resolveMerchantId(userId), chargebackId, request));
    }

    @PutMapping("/{chargebackId}/respond")
    public ApiResponse<ChargebackResponse> respond(
            @RequestParam UUID userId,
            @PathVariable UUID chargebackId,
            @Valid @RequestBody RespondChargebackRequest request) {
        return ApiResponse.success(chargebackService.respond(accessHelper.resolveMerchantId(userId), chargebackId, request));
    }
}
