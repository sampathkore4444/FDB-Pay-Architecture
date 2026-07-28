package com.fdbpay.corporate.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulk_disbursements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkDisbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "corporate_user_id", nullable = false)
    private UUID corporateUserId;

    @Column(name = "file_ref", nullable = false, length = 255)
    private String fileRef;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "total_rows", nullable = false)
    private int totalRows = 0;

    @Column(name = "successful_rows", nullable = false)
    private int successfulRows = 0;

    @Column(name = "failed_rows", nullable = false)
    private int failedRows = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
