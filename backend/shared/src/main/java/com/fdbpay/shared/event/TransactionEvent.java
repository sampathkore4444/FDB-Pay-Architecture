package com.fdbpay.shared.event;

import lombok.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent implements Serializable {
    private UUID transactionId;
    private String type;
    private String status;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private Long amount;
    private String currency;
    private UUID senderUserId;
    private UUID receiverUserId;
    private OffsetDateTime timestamp;
    private Map<String, Object> metadata;
}
