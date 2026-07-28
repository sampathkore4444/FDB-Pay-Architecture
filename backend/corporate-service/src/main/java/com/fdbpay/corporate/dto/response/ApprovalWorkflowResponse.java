package com.fdbpay.corporate.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalWorkflowResponse {

    private UUID id;
    private UUID bulkDisbursementId;
    private UUID approverUserId;
    private String status;
    private String comments;
    private OffsetDateTime approvedAt;
    private OffsetDateTime createdAt;
}
