package com.fdbpay.remittance.service.dto.response;

import com.fdbpay.remittance.service.model.enums.RemittanceStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemittanceResponse {

    private UUID id;
    private UUID recipientUserId;
    private String recipientPhone;
    private String senderName;
    private String senderCountry;
    private String corridor;
    private String partnerRef;
    private Long amount;
    private Long fee;
    private BigDecimal exchangeRate;
    private Long amountMmk;
    private RemittanceStatus status;
    private String referenceNumber;
    private OffsetDateTime receivedAt;
    private OffsetDateTime createdAt;
}
