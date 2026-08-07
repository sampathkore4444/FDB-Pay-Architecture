package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.NotificationTemplateRequest;
import com.fdbpay.merchant.service.dto.response.NotificationTemplateResponse;
import com.fdbpay.merchant.service.model.NotificationTemplate;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.NotificationTemplateRepository;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final MerchantRepository merchantRepository;

    public List<NotificationTemplateResponse> list(UUID merchantId) {
        requireMerchant(merchantId);
        return templateRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public NotificationTemplateResponse create(UUID merchantId, NotificationTemplateRequest request) {
        requireMerchant(merchantId);
        NotificationTemplate template = NotificationTemplate.builder()
                .merchantId(merchantId)
                .name(request.getName())
                .channel(request.getChannel() == null ? "SMS" : request.getChannel())
                .subject(request.getSubject())
                .body(request.getBody())
                .triggerEvent(request.getTriggerEvent())
                .enabled(request.getEnabled() == null || request.getEnabled())
                .build();
        template = templateRepository.save(template);
        return mapToResponse(template);
    }

    @Transactional
    public NotificationTemplateResponse update(UUID merchantId, UUID templateId, NotificationTemplateRequest request) {
        NotificationTemplate template = getOwned(merchantId, templateId);
        template.setName(request.getName());
        template.setChannel(request.getChannel() == null ? template.getChannel() : request.getChannel());
        template.setSubject(request.getSubject());
        template.setBody(request.getBody());
        template.setTriggerEvent(request.getTriggerEvent());
        template.setEnabled(request.getEnabled() == null || request.getEnabled());
        template = templateRepository.save(template);
        return mapToResponse(template);
    }

    @Transactional
    public void delete(UUID merchantId, UUID templateId) {
        NotificationTemplate template = getOwned(merchantId, templateId);
        templateRepository.delete(template);
    }

    private NotificationTemplate getOwned(UUID merchantId, UUID templateId) {
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", templateId.toString()));
        if (!template.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Template does not belong to this merchant");
        }
        return template;
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private NotificationTemplateResponse mapToResponse(NotificationTemplate template) {
        return NotificationTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .channel(template.getChannel())
                .subject(template.getSubject())
                .body(template.getBody())
                .triggerEvent(template.getTriggerEvent())
                .enabled(template.isEnabled())
                .createdAt(template.getCreatedAt())
                .build();
    }
}
