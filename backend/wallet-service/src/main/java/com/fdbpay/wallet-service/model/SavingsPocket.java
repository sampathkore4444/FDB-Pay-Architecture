package com.fdbpay.wallet.service.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "savings_pockets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsPocket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "goal_amount", nullable = false)
    private Long goalAmount;

    @Column(name = "current_amount", nullable = false)
    @Builder.Default
    private Long currentAmount = 0L;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal interestRate = new BigDecimal("0.0200");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private com.fdbpay.wallet.service.model.enums.PocketStatus status = com.fdbpay.wallet.service.model.enums.PocketStatus.ACTIVE;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
