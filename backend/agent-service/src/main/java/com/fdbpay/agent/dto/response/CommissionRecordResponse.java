package com.fdbpay.agent.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionRecordResponse {

    private UUID id;
    private UUID agentUserId;
    private UUID transactionId;
    private String type;
    private Long amount;
    private BigDecimal commissionRate;
    private Long commissionAmount;
    private String status;
    private OffsetDateTime earnedAt;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
}
