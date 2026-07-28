package com.fdbpay.corporate.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payroll_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "corporate_user_id", nullable = false)
    private UUID corporateUserId;

    @Column(nullable = false, length = 10)
    private String period;

    @Column(name = "total_employees", nullable = false)
    private int totalEmployees;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
