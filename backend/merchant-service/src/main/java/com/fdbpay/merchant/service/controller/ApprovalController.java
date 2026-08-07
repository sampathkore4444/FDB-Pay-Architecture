package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.response.ApprovalRequestResponse;
import com.fdbpay.merchant.service.model.enums.ApprovalStatus;
import com.fdbpay.merchant.service.service.ApprovalService;
import com.fdbpay.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping
    public ApiResponse<List<ApprovalRequestResponse>> list(@RequestParam UUID userId,
                                                           @RequestParam(required = false) ApprovalStatus status) {
        return ApiResponse.success(approvalService.listApprovals(accessHelper.resolveMerchantId(userId), status));
    }

    @PostMapping("/{approvalId}/approve")
    public ApiResponse<ApprovalRequestResponse> approve(@RequestParam UUID userId, @PathVariable UUID approvalId,
                                                        @RequestParam String reviewer) {
        return ApiResponse.success(approvalService.approve(accessHelper.resolveMerchantId(userId), approvalId, reviewer));
    }

    @PostMapping("/{approvalId}/reject")
    public ApiResponse<ApprovalRequestResponse> reject(@RequestParam UUID userId, @PathVariable UUID approvalId,
                                                       @RequestParam String reviewer) {
        return ApiResponse.success(approvalService.reject(accessHelper.resolveMerchantId(userId), approvalId, reviewer));
    }
}
