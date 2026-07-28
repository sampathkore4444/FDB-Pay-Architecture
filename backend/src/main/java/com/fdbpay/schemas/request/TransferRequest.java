package com.fdbpay.schemas.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class TransferRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    private String type;

    @NotBlank(message = "Recipient identifier is required")
    private String recipientIdentifier;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    private String description;

    private String recipientType;

    private Map<String, Object> metadata;
}
