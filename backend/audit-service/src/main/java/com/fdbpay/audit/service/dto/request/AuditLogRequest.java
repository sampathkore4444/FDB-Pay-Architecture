package com.fdbpay.audit.service.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogRequest {

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
}
