package com.fdbpay.transfer.service.dto.response.analytics;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsTransactionRow {

    private UUID id;
    private String direction;
    private String type;
    private String method;
    private Long amount;
    private Long fee;
    private String description;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private OffsetDateTime createdAt;
}
