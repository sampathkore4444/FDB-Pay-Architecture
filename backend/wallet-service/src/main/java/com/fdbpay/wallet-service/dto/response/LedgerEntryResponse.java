package com.fdbpay.wallet.service.dto.response;

import com.fdbpay.wallet.service.model.enums.LedgerEntryType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntryResponse {

    private UUID id;
    private LedgerEntryType type;
    private Long amount;
    private Long balanceAfter;
    private UUID txnId;
    private String description;
    private OffsetDateTime createdAt;
}
