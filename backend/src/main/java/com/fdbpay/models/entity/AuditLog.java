package com.fdbpay.models.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID actorId;

    @Column(nullable = false, length = 20)
    private String actorType;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String resourceType;

    @Column(length = 50)
    private String resourceId;

    @Column(columnDefinition = "jsonb")
    private String details;

    @Column(length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;
}
