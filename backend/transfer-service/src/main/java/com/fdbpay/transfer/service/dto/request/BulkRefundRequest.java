package com.fdbpay.transfer.service.dto.request;

import com.fdbpay.transfer.service.model.enums.RefundReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkRefundRequest {

    @NotNull(message = "Transaction ids are required")
    @NotEmpty(message = "At least one transaction id is required")
    private List<UUID> transactionIds;

    private String reason;

    private RefundReason reasonCode;

    private String note;

    private UUID staffId;

    private String staffName;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
