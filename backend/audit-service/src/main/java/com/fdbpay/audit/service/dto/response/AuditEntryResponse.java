package com.fdbpay.audit.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEntryResponse {

    private UUID id;
    private UUID actorId;
    private String actorType;
    private String actorName;
    private String action;
    private String resourceType;
    private String resourceId;
    private String oldValues;
    private String newValues;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    private OffsetDateTime createdAt;
}
