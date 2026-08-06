package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRuleRequest {

    @NotBlank(message = "Rule name is required")
    private String name;

    @NotNull(message = "Rule type is required")
    private String ruleType;

    @NotNull(message = "Threshold is required")
    @Positive(message = "Threshold must be positive")
    private Long threshold;
}
