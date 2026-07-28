package com.fdbpay.agent.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawCommissionRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;
}
