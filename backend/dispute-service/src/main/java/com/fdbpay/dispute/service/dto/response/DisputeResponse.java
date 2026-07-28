package com.fdbpay.dispute.service.dto.response;

import com.fdbpay.dispute.service.model.enums.DisputeStatus;
import com.fdbpay.dispute.service.model.enums.DisputeType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeResponse {

    private UUID id;
    private UUID transactionId;
    private UUID complainantUserId;
    private UUID respondentUserId;
    private DisputeType type;
    private DisputeStatus status;
    private Long amount;
    private String description;
    private String resolution;
    private UUID resolvedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime resolvedAt;
}
