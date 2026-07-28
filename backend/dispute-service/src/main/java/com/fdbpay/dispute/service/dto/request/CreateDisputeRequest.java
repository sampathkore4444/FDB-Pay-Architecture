package com.fdbpay.dispute.service.dto.request;

import com.fdbpay.dispute.service.model.enums.DisputeType;
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
public class CreateDisputeRequest {

    @NotBlank(message = "transactionId is required")
    private UUID transactionId;

    @NotNull(message = "dispute type is required")
    private DisputeType type;

    @Positive(message = "amount must be positive")
    private Long amount;

    @NotBlank(message = "description is required")
    private String description;
}
