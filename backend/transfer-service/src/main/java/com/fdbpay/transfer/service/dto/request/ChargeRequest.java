package com.fdbpay.transfer.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeRequest {

    @NotBlank(message = "Customer phone is required")
    private String customerPhone;

    private String customerName;

    @NotBlank(message = "Card number is required")
    private String cardLast4;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    private Long tipAmount;

    private Long taxAmount;

    private String description;

    private UUID staffId;

    private String staffName;

    private UUID storeId;

    private String discountCode;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
