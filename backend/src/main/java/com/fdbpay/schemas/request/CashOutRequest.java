package com.fdbpay.schemas.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CashOutRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotBlank(message = "Customer phone is required")
    private String customerPhone;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;
}
