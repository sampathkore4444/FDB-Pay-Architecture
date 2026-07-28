package com.fdbpay.settlement.service.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriggerSettlementRequest {

    private UUID merchantId;
}
