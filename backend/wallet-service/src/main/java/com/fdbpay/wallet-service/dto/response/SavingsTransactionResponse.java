package com.fdbpay.wallet.service.dto.response;

import com.fdbpay.wallet.service.model.enums.SavingsTxnType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsTransactionResponse {

    private UUID id;
    private UUID pocketId;
    private SavingsTxnType type;
    private Long amount;
    private Long balanceAfter;
    private String description;
    private OffsetDateTime createdAt;
}
