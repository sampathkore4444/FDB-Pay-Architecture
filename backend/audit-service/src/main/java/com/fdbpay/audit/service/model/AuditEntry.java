package com.fdbpay.audit.service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_entries", indexes = {
        @Index(name = "idx_audit_actor", columnList = "actorId"),
        @Index(name = "idx_audit_resource", columnList = "resourceType, resourceId"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_created_at", columnList = "createdAt")
})
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID actorId;

    @Column(nullable = false, length = 20)
    private String actorType;

    private String actorName;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String resourceType;

    private String resourceId;

    @Column(columnDefinition = "jsonb")
    private String oldValues;

    @Column(columnDefinition = "jsonb")
    private String newValues;

    private String ipAddress;

    private String userAgent;

    private String sessionId;

    @Builder.Default
    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
