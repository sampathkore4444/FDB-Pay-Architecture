package com.fdbpay.promotions.service.dto.response;

import com.fdbpay.promotions.service.model.enums.CashbackTxnType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashbackTransactionResponse {

    private UUID id;
    private UUID cashbackWalletId;
    private CashbackTxnType type;
    private Long amount;
    private UUID promotionId;
    private UUID transactionId;
    private String description;
    private OffsetDateTime createdAt;
}
