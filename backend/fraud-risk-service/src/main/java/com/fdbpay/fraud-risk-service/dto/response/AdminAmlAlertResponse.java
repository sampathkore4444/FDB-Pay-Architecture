package com.fdbpay.fraud.risk.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAmlAlertResponse {

    private UUID id;
    private UUID userId;
    private String type;
    private String severity;
    private String status;
    private String description;
    private OffsetDateTime createdAt;
}
