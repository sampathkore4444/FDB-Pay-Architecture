package com.fdbpay.wallet.service.model;

import com.fdbpay.wallet.service.model.enums.KycTier;
import com.fdbpay.wallet.service.model.enums.WalletStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "MMK";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Long balanceTotal = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long balanceHeld = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long balanceFrozen = 0L;

    @Builder.Default
    private Long dailyLimit = 500_000L;

    @Builder.Default
    private Long monthlyLimit = 5_000_000L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private KycTier kycTier = KycTier.NONE;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @Transient
    public Long getAvailableBalance() {
        return balanceTotal - balanceHeld - balanceFrozen;
    }
}
