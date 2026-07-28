package com.fdbpay.settlement.service.model;

import com.fdbpay.settlement.service.model.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "settlements", indexes = {
        @Index(name = "idx_settlement_merchant", columnList = "merchantId"),
        @Index(name = "idx_settlement_status", columnList = "status"),
        @Index(name = "idx_settlement_batch", columnList = "batchId"),
        @Index(name = "idx_settlement_period", columnList = "periodStart, periodEnd"),
        @Index(name = "idx_settlement_created_at", columnList = "createdAt")
})
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private UUID batchId;

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
    private SettlementStatus status;

    private OffsetDateTime settledAt;

    @Column(length = 50)
    private String settlementRef;

    @Column(nullable = false)
    private int transactionCount;

    @Builder.Default
    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
