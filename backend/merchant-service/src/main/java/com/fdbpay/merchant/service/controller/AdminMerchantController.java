package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.UpdateMerchantStatusRequest;
import com.fdbpay.merchant.service.dto.response.MerchantResponse;
import com.fdbpay.merchant.service.model.enums.MerchantStatus;
import com.fdbpay.merchant.service.service.MerchantService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/merchants")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final MerchantService merchantService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getMerchants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MerchantStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<MerchantResponse> merchants = merchantService.getMerchants(search, status, page, size);
        return ApiResponse.success(Map.of("merchants", merchants));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<MerchantResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMerchantStatusRequest request) {
        return ApiResponse.success(merchantService.updateStatus(id, request.getStatus(), request.getReason()));
    }
}
