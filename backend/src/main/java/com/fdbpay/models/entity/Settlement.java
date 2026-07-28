package com.fdbpay.models.entity;

import com.fdbpay.models.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false)
    private OffsetDateTime periodStart;

    @Column(nullable = false)
    private OffsetDateTime periodEnd;

    @Column(nullable = false)
    private Long grossAmount;

    @Column(nullable = false)
    private Long fees;

    @Column(nullable = false)
    private Long netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    private OffsetDateTime settledAt;

    @Column(length = 50)
    private String settlementRef;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;
}
