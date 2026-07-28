package com.fdbpay.agent.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTransactionResponse {

    private UUID id;
    private UUID agentUserId;
    private String customerPhone;
    private String type;
    private Long amount;
    private String status;
    private OffsetDateTime createdAt;
}
