package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutRequest {

    @NotNull(message = "Account is required")
    private UUID accountId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;
}
