package com.fdbpay.agent.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentQrResponse {

    private String qrData;
    private UUID agentUserId;
    private String agentName;
}
