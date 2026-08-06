package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.ApiKeyRequest;
import com.fdbpay.merchant.service.dto.request.ReportTemplateRequest;
import com.fdbpay.merchant.service.dto.response.ApiKeyResponse;
import com.fdbpay.merchant.service.dto.response.ReportTemplateResponse;
import com.fdbpay.merchant.service.model.ApiKey;
import com.fdbpay.merchant.service.model.ReportTemplate;
import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import com.fdbpay.merchant.service.repository.ApiKeyRepository;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.ReportTemplateRepository;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeveloperService {

    private final ApiKeyRepository apiKeyRepository;
    private final ReportTemplateRepository reportTemplateRepository;
    private final MerchantRepository merchantRepository;
    private final AuditService auditService;

    // ---- API keys ----

    public List<ApiKeyResponse> listApiKeys(UUID merchantId) {
        requireMerchant(merchantId);
        return apiKeyRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream().map(this::mapApiKey).toList();
    }

    @Transactional
    public ApiKeyResponse createApiKey(UUID merchantId, ApiKeyRequest request) {
        requireMerchant(merchantId);
        boolean sandbox = "SANDBOX".equalsIgnoreCase(request.getEnvironment());
        String raw = (sandbox ? "fdb_test_" : "fdb_live_")
                + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().substring(0, 12);
        ApiKey key = ApiKey.builder()
                .merchantId(merchantId)
                .name(request.getName())
                .keyHash(hash(raw))
                .keyPreview(raw.substring(0, 14) + "...")
                .environment(sandbox ? "SANDBOX" : "LIVE")
                .usageCount(0L)
                .status(ActiveStatus.ACTIVE)
                .build();
        key = apiKeyRepository.save(key);
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "API_KEY", key.getId().toString(),
                "Created " + key.getEnvironment() + " API key '" + request.getName() + "'");
        return mapApiKey(key);
    }

    @Transactional
    public ApiKeyResponse recordUsage(UUID merchantId, UUID keyId) {
        ApiKey key = getOwnedKey(merchantId, keyId);
        key.setUsageCount(key.getUsageCount() == null ? 1L : key.getUsageCount() + 1);
        key.setLastUsedAt(java.time.OffsetDateTime.now());
        key = apiKeyRepository.save(key);
        return mapApiKey(key);
    }

    @Transactional
    public void revokeApiKey(UUID merchantId, UUID keyId) {
        ApiKey key = getOwnedKey(merchantId, keyId);
        key.setStatus(ActiveStatus.INACTIVE);
        apiKeyRepository.save(key);
        auditService.log(merchantId, "OWNER", null, null, "REVOKE", "API_KEY", keyId.toString(), "Revoked API key '" + key.getName() + "'");
    }

    // ---- Report templates ----

    public List<ReportTemplateResponse> listReportTemplates(UUID merchantId) {
        requireMerchant(merchantId);
        return reportTemplateRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream().map(this::mapTemplate).toList();
    }

    @Transactional
    public ReportTemplateResponse createReportTemplate(UUID merchantId, ReportTemplateRequest request) {
        requireMerchant(merchantId);
        ReportTemplate template = ReportTemplate.builder()
                .merchantId(merchantId)
                .name(request.getName())
                .reportType(request.getReportType())
                .frequency(request.getFrequency())
                .format(request.getFormat() == null ? "CSV" : request.getFormat())
                .email(request.getEmail())
                .enabled(request.getEnabled() == null || request.getEnabled())
                .build();
        template = reportTemplateRepository.save(template);
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "REPORT_TEMPLATE", template.getId().toString(),
                "Created report template '" + template.getName() + "'");
        return mapTemplate(template);
    }

    @Transactional
    public void deleteReportTemplate(UUID merchantId, UUID templateId) {
        ReportTemplate template = getOwnedTemplate(merchantId, templateId);
        reportTemplateRepository.delete(template);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR, "Failed to hash API key");
        }
    }

    private ApiKey getOwnedKey(UUID merchantId, UUID keyId) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", keyId.toString()));
        if (!key.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "API key does not belong to this merchant");
        }
        return key;
    }

    private ReportTemplate getOwnedTemplate(UUID merchantId, UUID templateId) {
        ReportTemplate template = reportTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("ReportTemplate", templateId.toString()));
        if (!template.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Report template does not belong to this merchant");
        }
        return template;
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private ApiKeyResponse mapApiKey(ApiKey key) {
        return ApiKeyResponse.builder()
                .id(key.getId())
                .name(key.getName())
                .keyPreview(key.getKeyPreview())
                .environment(key.getEnvironment())
                .usageCount(key.getUsageCount())
                .status(key.getStatus())
                .lastUsedAt(key.getLastUsedAt())
                .createdAt(key.getCreatedAt())
                .build();
    }

    private ReportTemplateResponse mapTemplate(ReportTemplate template) {
        return ReportTemplateResponse.builder()
                .id(template.getId())
                .merchantId(template.getMerchantId())
                .name(template.getName())
                .reportType(template.getReportType())
                .frequency(template.getFrequency())
                .format(template.getFormat())
                .email(template.getEmail())
                .enabled(template.getEnabled())
                .createdAt(template.getCreatedAt())
                .build();
    }
}
