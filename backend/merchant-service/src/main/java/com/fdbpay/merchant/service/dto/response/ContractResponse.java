package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.SettlementType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractResponse {

    private SettlementType settlementType;
    private BigDecimal feeRate;
    private Integer settlementFrequencyDays;
    private Long rollingReserveRate;
}
