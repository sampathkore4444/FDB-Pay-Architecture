package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.PosTerminalStatus;
import com.fdbpay.merchant.service.model.enums.PosTerminalType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosTerminalResponse {

    private UUID id;
    private UUID merchantId;
    private String serialNumber;
    private PosTerminalType type;
    private PosTerminalStatus status;
    private OffsetDateTime lastPingAt;
    private OffsetDateTime createdAt;
}
