package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.ChargebackReason;
import com.fdbpay.merchant.service.model.enums.ChargebackStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargebackResponse {

    private UUID id;
    private UUID merchantId;
    private UUID transactionId;
    private Long amount;
    private String currency;
    private ChargebackReason reasonCode;
    private ChargebackStatus status;
    private String customerNotes;
    private OffsetDateTime deadline;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<ChargebackNoteResponse> notes;
}
