package com.fdbpay.transfer.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.transfer.service.dto.request.BulkRefundRequest;
import com.fdbpay.transfer.service.dto.request.BulkVoidRequest;
import com.fdbpay.transfer.service.dto.request.ChargeRequest;
import com.fdbpay.transfer.service.dto.response.BulkOperationResponse;
import com.fdbpay.transfer.service.dto.response.TransactionResponse;
import com.fdbpay.transfer.service.dto.response.analytics.MerchantStatement;
import com.fdbpay.transfer.service.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class MerchantPortalController {

    private final TransferService transferService;

    @PostMapping("/charge")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> charge(
            @RequestParam UUID merchantUserId,
            @Valid @RequestBody ChargeRequest request) {
        return ApiResponse.success(transferService.charge(merchantUserId, request));
    }

    @PostMapping("/refunds")
    public ApiResponse<BulkOperationResponse> bulkRefund(
            @RequestParam UUID merchantUserId,
            @Valid @RequestBody BulkRefundRequest request) {
        return ApiResponse.success(transferService.bulkRefund(merchantUserId, request));
    }

    @PostMapping("/voids")
    public ApiResponse<BulkOperationResponse> bulkVoid(
            @RequestParam UUID merchantUserId,
            @Valid @RequestBody BulkVoidRequest request) {
        return ApiResponse.success(transferService.bulkVoid(merchantUserId, request));
    }

    @GetMapping("/statements/merchant")
    public ApiResponse<MerchantStatement> getStatement(
            @RequestParam UUID walletId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer rollingReservePercent,
            @RequestParam(required = false) Integer rollingReservePeriodDays) {
        return ApiResponse.success(transferService.getStatement(
                walletId, from, to, rollingReservePercent, rollingReservePeriodDays));
    }
}
