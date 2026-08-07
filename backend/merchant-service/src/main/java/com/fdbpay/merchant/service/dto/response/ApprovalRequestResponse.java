package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.ApprovalStatus;
import com.fdbpay.merchant.service.model.enums.ApprovalType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequestResponse {

    private UUID id;
    private ApprovalType type;
    private Long amount;
    private UUID refId;
    private String initiatorName;
    private ApprovalStatus status;
    private String reviewedBy;
    private OffsetDateTime reviewedAt;
    private OffsetDateTime createdAt;
}
