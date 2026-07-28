package com.fdbpay.merchant.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.merchant.service.dto.response.MerchantDirectoryEntry;
import com.fdbpay.merchant.service.service.MerchantDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/merchants/directory")
@RequiredArgsConstructor
public class MerchantDirectoryController {

    private final MerchantDirectoryService merchantDirectoryService;

    @GetMapping("/search")
    public ApiResponse<Page<MerchantDirectoryEntry>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radius,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(merchantDirectoryService.search(
                query, category, latitude, longitude, radius, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<MerchantDirectoryEntry> getMerchantDetails(@PathVariable UUID id) {
        return ApiResponse.success(merchantDirectoryService.getMerchantDetails(id));
    }

    @GetMapping("/nearby")
    public ApiResponse<Page<MerchantDirectoryEntry>> getNearbyMerchants(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5.0") Double radius,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(merchantDirectoryService.getNearbyMerchants(
                latitude, longitude, radius, page, size));
    }
}
