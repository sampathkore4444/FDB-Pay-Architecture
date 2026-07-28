package com.fdbpay.fraud.risk.service.dto.response;

import com.fdbpay.fraud.risk.service.model.enums.AlertSeverity;
import com.fdbpay.fraud.risk.service.model.enums.AlertStatus;
import com.fdbpay.fraud.risk.service.model.enums.AlertType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlertResponse {

    private UUID id;
    private UUID transactionId;
    private UUID userId;
    private AlertType alertType;
    private AlertSeverity severity;
    private AlertStatus status;
    private String details;
    private OffsetDateTime createdAt;
    private OffsetDateTime resolvedAt;
}
