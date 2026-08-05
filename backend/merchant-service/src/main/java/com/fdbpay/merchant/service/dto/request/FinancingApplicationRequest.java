package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancingApplicationRequest {

    @NotNull(message = "Requested amount is required")
    @Positive(message = "Requested amount must be positive")
    private Long requestedAmount;

    @NotNull(message = "Term months is required")
    @Min(value = 1, message = "Term must be at least 1 month")
    @Max(value = 24, message = "Term must be at most 24 months")
    private Integer termMonths;

    private String purpose;
}
