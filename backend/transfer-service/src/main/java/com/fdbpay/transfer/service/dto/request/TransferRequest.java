package com.fdbpay.transfer.service.dto.request;

import com.fdbpay.transfer.service.model.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotBlank(message = "Recipient identifier is required")
    private String recipientIdentifier;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    private String description;

    private String recipientType;
}
