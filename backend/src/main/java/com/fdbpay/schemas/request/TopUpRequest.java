package com.fdbpay.schemas.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TopUpRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    @NotBlank(message = "Top-up channel is required")
    private String channel;

    private String reference;
}
