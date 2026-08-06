package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.FraudRuleRequest;
import com.fdbpay.merchant.service.dto.response.FraudRuleResponse;
import com.fdbpay.merchant.service.model.FraudRule;
import com.fdbpay.merchant.service.model.enums.FraudRuleType;
import com.fdbpay.merchant.service.repository.FraudRuleRepository;
import com.fdbpay.merchant.service.repository.MerchantRepository;
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
public class FraudRuleService {

    private final FraudRuleRepository fraudRuleRepository;
    private final MerchantRepository merchantRepository;
    private final AuditService auditService;

    public List<FraudRuleResponse> listRules(UUID merchantId) {
        requireMerchant(merchantId);
        return fraudRuleRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream().map(this::mapRule).toList();
    }

    @Transactional
    public FraudRuleResponse createRule(UUID merchantId, FraudRuleRequest request) {
        requireMerchant(merchantId);
        FraudRuleType type = parseType(request.getRuleType());
        FraudRule rule = FraudRule.builder()
                .merchantId(merchantId)
                .name(request.getName())
                .ruleType(type)
                .threshold(request.getThreshold())
                .enabled(true)
                .build();
        rule = fraudRuleRepository.save(rule);
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "FRAUD_RULE", rule.getId().toString(),
                "Created fraud rule '" + rule.getName() + "' (" + type + " >= " + rule.getThreshold() + ")");
        return mapRule(rule);
    }

    @Transactional
    public FraudRuleResponse toggleRule(UUID merchantId, UUID ruleId) {
        FraudRule rule = getOwned(merchantId, ruleId);
        rule.setEnabled(!rule.isEnabled());
        rule = fraudRuleRepository.save(rule);
        return mapRule(rule);
    }

    @Transactional
    public void deleteRule(UUID merchantId, UUID ruleId) {
        FraudRule rule = getOwned(merchantId, ruleId);
        fraudRuleRepository.delete(rule);
        auditService.log(merchantId, "OWNER", null, null, "DELETE", "FRAUD_RULE", ruleId.toString(),
                "Deleted fraud rule '" + rule.getName() + "'");
    }

    private FraudRuleType parseType(String raw) {
        try {
            return FraudRuleType.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Unsupported fraud rule type: " + raw);
        }
    }

    private FraudRule getOwned(UUID merchantId, UUID ruleId) {
        FraudRule rule = fraudRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudRule", ruleId.toString()));
        if (!rule.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Rule does not belong to this merchant");
        }
        return rule;
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private FraudRuleResponse mapRule(FraudRule rule) {
        return FraudRuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .ruleType(rule.getRuleType())
                .threshold(rule.getThreshold())
                .enabled(rule.isEnabled())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}
