package com.fdbpay.dispute.service.model;

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
@Table(name = "dispute_evidence", indexes = {
        @Index(name = "idx_evidence_dispute", columnList = "disputeId"),
        @Index(name = "idx_evidence_uploaded_by", columnList = "uploadedBy")
})
public class DisputeEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID disputeId;

    @Column(nullable = false)
    private UUID uploadedBy;

    @Column(nullable = false)
    private String fileUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
