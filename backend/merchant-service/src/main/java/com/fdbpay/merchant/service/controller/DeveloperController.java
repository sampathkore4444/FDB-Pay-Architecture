package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.ApiKeyRequest;
import com.fdbpay.merchant.service.dto.request.ReportTemplateRequest;
import com.fdbpay.merchant.service.dto.request.WebhookSubscriptionRequest;
import com.fdbpay.merchant.service.dto.response.ApiKeyResponse;
import com.fdbpay.merchant.service.dto.response.ReportTemplateResponse;
import com.fdbpay.merchant.service.dto.response.WebhookDeliveryResponse;
import com.fdbpay.merchant.service.dto.response.WebhookSubscriptionResponse;
import com.fdbpay.merchant.service.service.DeveloperService;
import com.fdbpay.merchant.service.service.WebhookService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/developer")
@RequiredArgsConstructor
public class DeveloperController {

    private final DeveloperService developerService;
    private final WebhookService webhookService;
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

    @PutMapping("/api-keys/{keyId}/usage")
    public ApiResponse<ApiKeyResponse> recordUsage(@RequestParam UUID userId, @PathVariable UUID keyId) {
        return ApiResponse.success(developerService.recordUsage(accessHelper.resolveMerchantId(userId), keyId));
    }

    // ---- Webhooks ----

    @GetMapping("/webhooks")
    public ApiResponse<List<WebhookSubscriptionResponse>> listWebhooks(@RequestParam UUID userId) {
        return ApiResponse.success(webhookService.listSubscriptions(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping("/webhooks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WebhookSubscriptionResponse> createWebhook(@RequestParam UUID userId, @Valid @RequestBody WebhookSubscriptionRequest request) {
        return ApiResponse.success(webhookService.createSubscription(accessHelper.resolveMerchantId(userId), request));
    }

    @PutMapping("/webhooks/{subscriptionId}/toggle")
    public ApiResponse<WebhookSubscriptionResponse> toggleWebhook(@RequestParam UUID userId, @PathVariable UUID subscriptionId) {
        return ApiResponse.success(webhookService.toggleSubscription(accessHelper.resolveMerchantId(userId), subscriptionId));
    }

    @DeleteMapping("/webhooks/{subscriptionId}")
    public ApiResponse<Void> deleteWebhook(@RequestParam UUID userId, @PathVariable UUID subscriptionId) {
        webhookService.deleteSubscription(accessHelper.resolveMerchantId(userId), subscriptionId);
        return ApiResponse.success(null);
    }

    @PostMapping("/webhooks/{subscriptionId}/test")
    public ApiResponse<WebhookDeliveryResponse> testWebhook(@RequestParam UUID userId, @PathVariable UUID subscriptionId) {
        return ApiResponse.success(webhookService.sendTest(accessHelper.resolveMerchantId(userId), subscriptionId));
    }

    @GetMapping("/webhooks/deliveries")
    public ApiResponse<Page<WebhookDeliveryResponse>> listDeliveries(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(webhookService.listDeliveries(accessHelper.resolveMerchantId(userId), page, size));
    }

    @PostMapping("/webhooks/deliveries/{deliveryId}/replay")
    public ApiResponse<WebhookDeliveryResponse> replayDelivery(@RequestParam UUID userId, @PathVariable UUID deliveryId) {
        return ApiResponse.success(webhookService.replay(accessHelper.resolveMerchantId(userId), deliveryId));
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
