package com.fdbpay.transfer.service.dto.request;

import com.fdbpay.transfer.service.model.ScheduledPayment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateScheduledPaymentRequest {

    @NotBlank(message = "Recipient identifier is required")
    private String recipientIdentifier;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    @NotNull(message = "Transaction type is required")
    private ScheduledPayment.TransactionType type;

    @NotNull(message = "Frequency is required")
    private com.fdbpay.transfer.service.model.enums.PaymentFrequency frequency;

    private String description;
}
