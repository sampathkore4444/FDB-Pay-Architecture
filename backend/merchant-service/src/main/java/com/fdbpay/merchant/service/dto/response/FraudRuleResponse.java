package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.FraudRuleType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRuleResponse {

    private UUID id;
    private String name;
    private FraudRuleType ruleType;
    private Long threshold;
    private boolean enabled;
    private OffsetDateTime createdAt;
}
