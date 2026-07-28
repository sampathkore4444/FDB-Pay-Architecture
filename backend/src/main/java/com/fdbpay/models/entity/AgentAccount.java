package com.fdbpay.models.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false)
    @Builder.Default
    private Long floatBalance = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long commissionBalance = 0L;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Builder.Default
    private Long dailyLimit = 50_000_000L;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;
}
