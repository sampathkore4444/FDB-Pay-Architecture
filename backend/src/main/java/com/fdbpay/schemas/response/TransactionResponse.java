package com.fdbpay.schemas.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private UUID id;
    private String idempotencyKey;
    private String type;
    private String status;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private Long amount;
    private Long fee;
    private String currency;
    private String description;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
    private String failureReason;
}
