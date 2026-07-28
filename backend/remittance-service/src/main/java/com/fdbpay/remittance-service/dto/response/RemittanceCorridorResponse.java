package com.fdbpay.remittance.service.dto.response;

import com.fdbpay.remittance.service.model.enums.RemittanceCorridorStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemittanceCorridorResponse {

    private UUID id;
    private String code;
    private String sourceCountry;
    private String sourceCurrency;
    private String destCurrency;
    private BigDecimal exchangeRate;
    private Long feeFixed;
    private BigDecimal feePercentage;
    private Long minAmount;
    private Long maxAmount;
    private String partnerName;
    private RemittanceCorridorStatus status;
    private OffsetDateTime createdAt;
}
