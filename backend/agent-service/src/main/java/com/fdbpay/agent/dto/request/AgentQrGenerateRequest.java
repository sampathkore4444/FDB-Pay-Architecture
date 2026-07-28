package com.fdbpay.agent.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentQrGenerateRequest {

    private Long amount;
}
