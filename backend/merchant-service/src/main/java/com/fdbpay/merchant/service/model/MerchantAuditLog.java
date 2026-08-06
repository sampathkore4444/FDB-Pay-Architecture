package com.fdbpay.merchant.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "merchant_audit_log", indexes = {
        @Index(name = "idx_audit_merchant", columnList = "merchantId,createdAt")
})
public class MerchantAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    private String actorType;

    private String actorName;

    private UUID staffId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 50)
    private String entity;

    @Column(length = 50)
    private String entityId;

    @Column(columnDefinition = "text")
    private String details;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
