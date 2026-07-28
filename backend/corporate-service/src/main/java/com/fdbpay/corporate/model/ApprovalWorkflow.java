package com.fdbpay.corporate.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "approval_workflows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bulk_disbursement_id", nullable = false)
    private UUID bulkDisbursementId;

    @Column(name = "approver_user_id", nullable = false)
    private UUID approverUserId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String comments;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
