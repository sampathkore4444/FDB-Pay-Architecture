package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.NotificationTemplateRequest;
import com.fdbpay.merchant.service.dto.response.NotificationTemplateResponse;
import com.fdbpay.merchant.service.service.NotificationTemplateService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/notification-templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping
    public ApiResponse<List<NotificationTemplateResponse>> list(@RequestParam UUID userId) {
        return ApiResponse.success(templateService.list(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotificationTemplateResponse> create(@RequestParam UUID userId,
                                                            @Valid @RequestBody NotificationTemplateRequest request) {
        return ApiResponse.success(templateService.create(accessHelper.resolveMerchantId(userId), request));
    }

    @PutMapping("/{templateId}")
    public ApiResponse<NotificationTemplateResponse> update(@RequestParam UUID userId, @PathVariable UUID templateId,
                                                            @Valid @RequestBody NotificationTemplateRequest request) {
        return ApiResponse.success(templateService.update(accessHelper.resolveMerchantId(userId), templateId, request));
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> delete(@RequestParam UUID userId, @PathVariable UUID templateId) {
        templateService.delete(accessHelper.resolveMerchantId(userId), templateId);
        return ApiResponse.success(null);
    }
}
