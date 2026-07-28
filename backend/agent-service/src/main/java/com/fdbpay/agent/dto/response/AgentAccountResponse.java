package com.fdbpay.agent.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentAccountResponse {

    private UUID id;
    private UUID userId;
    private UUID walletId;
    private Long floatBalance;
    private Long commissionBalance;
    private String status;
    private Long dailyLimit;
    private OffsetDateTime createdAt;
}
