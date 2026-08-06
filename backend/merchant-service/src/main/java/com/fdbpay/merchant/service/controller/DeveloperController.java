package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.ApiKeyRequest;
import com.fdbpay.merchant.service.dto.request.ReportTemplateRequest;
import com.fdbpay.merchant.service.dto.response.ApiKeyResponse;
import com.fdbpay.merchant.service.dto.response.ReportTemplateResponse;
import com.fdbpay.merchant.service.service.DeveloperService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/developer")
@RequiredArgsConstructor
public class DeveloperController {

    private final DeveloperService developerService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping("/api-keys")
    public ApiResponse<List<ApiKeyResponse>> listApiKeys(@RequestParam UUID userId) {
        return ApiResponse.success(developerService.listApiKeys(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping("/api-keys")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApiKeyResponse> createApiKey(@RequestParam UUID userId, @Valid @RequestBody ApiKeyRequest request) {
        return ApiResponse.success(developerService.createApiKey(accessHelper.resolveMerchantId(userId), request));
    }

    @PutMapping("/api-keys/{keyId}/revoke")
    public ApiResponse<Void> revokeApiKey(@RequestParam UUID userId, @PathVariable UUID keyId) {
        developerService.revokeApiKey(accessHelper.resolveMerchantId(userId), keyId);
        return ApiResponse.success(null);
    }

    @GetMapping("/report-templates")
    public ApiResponse<List<ReportTemplateResponse>> listReportTemplates(@RequestParam UUID userId) {
        return ApiResponse.success(developerService.listReportTemplates(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping("/report-templates")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportTemplateResponse> createReportTemplate(@RequestParam UUID userId, @Valid @RequestBody ReportTemplateRequest request) {
        return ApiResponse.success(developerService.createReportTemplate(accessHelper.resolveMerchantId(userId), request));
    }

    @DeleteMapping("/report-templates/{templateId}")
    public ApiResponse<Void> deleteReportTemplate(@RequestParam UUID userId, @PathVariable UUID templateId) {
        developerService.deleteReportTemplate(accessHelper.resolveMerchantId(userId), templateId);
        return ApiResponse.success(null);
    }
}
