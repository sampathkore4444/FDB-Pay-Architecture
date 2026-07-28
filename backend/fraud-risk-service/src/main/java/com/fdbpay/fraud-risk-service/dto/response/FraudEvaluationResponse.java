package com.fdbpay.fraud.risk.service.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudEvaluationResponse {

    private boolean approved;
    private int riskScore;
    private List<String> reasons;
}
