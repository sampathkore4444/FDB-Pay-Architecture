package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.response.AuditLogResponse;
import com.fdbpay.merchant.service.service.AuditService;
import com.fdbpay.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping
    public ApiResponse<List<AuditLogResponse>> list(
            @RequestParam UUID userId,
            @RequestParam(required = false) UUID staffId) {
        return ApiResponse.success(auditService.list(accessHelper.resolveMerchantId(userId), staffId));
    }
}
