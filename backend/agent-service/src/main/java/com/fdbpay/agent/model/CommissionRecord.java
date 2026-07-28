package com.fdbpay.agent.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "commission_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agent_user_id", nullable = false)
    private UUID agentUserId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "commission_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false)
    private Long commissionAmount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "earned_at", nullable = false)
    private OffsetDateTime earnedAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
