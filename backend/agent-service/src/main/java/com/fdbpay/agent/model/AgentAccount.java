package com.fdbpay.agent.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "float_balance", nullable = false)
    private Long floatBalance = 0L;

    @Column(name = "commission_balance", nullable = false)
    private Long commissionBalance = 0L;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "daily_limit", nullable = false)
    private Long dailyLimit = 50_000_000L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
