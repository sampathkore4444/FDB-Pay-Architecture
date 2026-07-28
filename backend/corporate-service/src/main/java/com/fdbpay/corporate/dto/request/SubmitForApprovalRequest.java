package com.fdbpay.corporate.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitForApprovalRequest {

    @NotNull(message = "Bulk disbursement ID is required")
    private UUID bulkDisbursementId;
}
