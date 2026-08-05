package com.fdbpay.merchant.service.dto.request;

import com.fdbpay.merchant.service.model.enums.ChargebackReason;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddChargebackRequest {

    private UUID transactionId;

    @NotNull(message = "Amount is required")
    private Long amount;

    private ChargebackReason reasonCode;

    private String customerNotes;

    private OffsetDateTime deadline;
}
