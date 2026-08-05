package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.RiskAlertStatus;
import com.fdbpay.merchant.service.model.enums.RiskSeverity;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAlertResponse {

    private UUID id;
    private UUID merchantId;
    private String alertType;
    private RiskSeverity severity;
    private String title;
    private String message;
    private RiskAlertStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime acknowledgedAt;
}
