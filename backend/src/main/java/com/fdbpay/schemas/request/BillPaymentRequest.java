package com.fdbpay.schemas.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BillPaymentRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotBlank(message = "Biller ID is required")
    private String billerId;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    private String description;
}
