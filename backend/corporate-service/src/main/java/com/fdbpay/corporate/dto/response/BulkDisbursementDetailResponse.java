package com.fdbpay.corporate.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkDisbursementDetailResponse {

    private UUID id;
    private UUID corporateUserId;
    private String fileRef;
    private String description;
    private String status;
    private int totalRows;
    private int successfulRows;
    private int failedRows;
    private String approvalStatus;
    private List<ApprovalWorkflowResponse> approvals;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
}
