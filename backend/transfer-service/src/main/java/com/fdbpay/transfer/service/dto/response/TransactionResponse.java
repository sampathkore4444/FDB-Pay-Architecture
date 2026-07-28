package com.fdbpay.transfer.service.dto.response;

import com.fdbpay.transfer.service.model.enums.TransactionStatus;
import com.fdbpay.transfer.service.model.enums.TransactionType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private UUID id;
    private String idempotencyKey;
    private TransactionType type;
    private TransactionStatus status;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private Long amount;
    private Long fee;
    private String currency;
    private String description;
    private String metadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
    private String failureReason;
}
