package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.RefundRequest;
import com.fdbpay.merchant.service.dto.response.RefundResponse;
import com.fdbpay.merchant.service.model.enums.RefundStatus;
import com.fdbpay.merchant.service.service.RefundService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping
    public ApiResponse<List<RefundResponse>> list(@RequestParam UUID userId,
                                                  @RequestParam(required = false) RefundStatus status) {
        return ApiResponse.success(refundService.listRefunds(accessHelper.resolveMerchantId(userId), status));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RefundResponse> create(@RequestParam UUID userId, @Valid @RequestBody RefundRequest request) {
        return ApiResponse.success(refundService.createRefund(accessHelper.resolveMerchantId(userId), request));
    }
}
