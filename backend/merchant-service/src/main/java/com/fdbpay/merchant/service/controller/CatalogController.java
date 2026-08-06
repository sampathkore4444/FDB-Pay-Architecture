package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.ProductRequest;
import com.fdbpay.merchant.service.dto.response.ProductResponse;
import com.fdbpay.merchant.service.service.CatalogService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/products")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping
    public ApiResponse<List<ProductResponse>> list(@RequestParam UUID userId) {
        return ApiResponse.success(catalogService.listProducts(accessHelper.resolveMerchantId(userId)));
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<ProductResponse>> lowStock(@RequestParam UUID userId) {
        return ApiResponse.success(catalogService.lowStock(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(@RequestParam UUID userId, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(catalogService.createProduct(accessHelper.resolveMerchantId(userId), request));
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> update(@RequestParam UUID userId, @PathVariable UUID productId,
                                               @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(catalogService.updateProduct(accessHelper.resolveMerchantId(userId), productId, request));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> delete(@RequestParam UUID userId, @PathVariable UUID productId) {
        catalogService.deleteProduct(accessHelper.resolveMerchantId(userId), productId);
        return ApiResponse.success(null);
    }
}
