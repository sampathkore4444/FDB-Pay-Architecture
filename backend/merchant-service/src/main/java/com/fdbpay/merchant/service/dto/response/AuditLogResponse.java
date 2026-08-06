package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private UUID id;
    private String actorType;
    private String actorName;
    private UUID staffId;
    private String action;
    private String entity;
    private String entityId;
    private String details;
    private OffsetDateTime createdAt;
}
