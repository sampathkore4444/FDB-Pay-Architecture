package com.fdbpay.settlement.service.dto.request;

import com.fdbpay.settlement.service.model.enums.SettlementStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementQueryRequest {

    private LocalDate startDate;
    private LocalDate endDate;
    private SettlementStatus status;
}
