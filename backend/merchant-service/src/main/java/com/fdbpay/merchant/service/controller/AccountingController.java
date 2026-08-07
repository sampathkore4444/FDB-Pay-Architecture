package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.response.AccountingExportResponse;
import com.fdbpay.merchant.service.service.AccountingService;
import com.fdbpay.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingService accountingService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping("/export")
    public ApiResponse<List<AccountingExportResponse>> export(
            @RequestParam UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return ApiResponse.success(accountingService.export(accessHelper.resolveMerchantId(userId), from, to));
    }
}
