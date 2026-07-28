package com.fdbpay.models.entity;

import com.fdbpay.models.enums.KycTier;
import com.fdbpay.models.enums.WalletStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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
    @Column(length = 20)
    @Builder.Default
    private KycTier kycTier = KycTier.BASIC;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    @Transient
    public Long getAvailableBalance() {
        return balanceTotal - balanceHeld - balanceFrozen;
    }
}
