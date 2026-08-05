package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.StoreRequest;
import com.fdbpay.merchant.service.dto.response.StoreResponse;
import com.fdbpay.merchant.service.service.StoreService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping
    public ApiResponse<List<StoreResponse>> getStores(@RequestParam UUID userId) {
        return ApiResponse.success(storeService.getStoresByMerchant(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StoreResponse> createStore(
            @RequestParam UUID userId,
            @Valid @RequestBody StoreRequest request) {
        return ApiResponse.success(storeService.createStore(accessHelper.resolveMerchantId(userId), request));
    }

    @PutMapping("/{storeId}")
    public ApiResponse<StoreResponse> updateStore(
            @RequestParam UUID userId,
            @PathVariable UUID storeId,
            @Valid @RequestBody StoreRequest request) {
        return ApiResponse.success(storeService.updateStore(accessHelper.resolveMerchantId(userId), storeId, request));
    }
}
