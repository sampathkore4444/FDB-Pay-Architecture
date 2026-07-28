package com.fdbpay.dispute.service.model;

import com.fdbpay.dispute.service.model.enums.DisputeStatus;
import com.fdbpay.dispute.service.model.enums.DisputeType;
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
@Table(name = "disputes", indexes = {
        @Index(name = "idx_dispute_transaction", columnList = "transactionId"),
        @Index(name = "idx_dispute_complainant", columnList = "complainantUserId"),
        @Index(name = "idx_dispute_respondent", columnList = "respondentUserId"),
        @Index(name = "idx_dispute_status", columnList = "status"),
        @Index(name = "idx_dispute_created_at", columnList = "createdAt")
})
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID transactionId;

    @Column(nullable = false)
    private UUID complainantUserId;

    @Column(nullable = false)
    private UUID respondentUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DisputeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DisputeStatus status;

    @Column(nullable = false)
    private Long amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    private UUID resolvedBy;

    @Builder.Default
    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    private OffsetDateTime resolvedAt;
}
