package com.fdbpay.settlement.service.model;

import com.fdbpay.settlement.service.model.enums.BatchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "settlement_batches", indexes = {
        @Index(name = "idx_batch_status", columnList = "status"),
        @Index(name = "idx_batch_date", columnList = "batchDate", unique = true)
})
public class SettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private LocalDate batchDate;

    @Column(nullable = false)
    private int totalMerchants;

    @Column(nullable = false)
    private Long totalGrossAmount;

    @Column(nullable = false)
    private Long totalFees;

    @Column(nullable = false)
    private Long totalNetAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchStatus status;

    private OffsetDateTime startedAt;

    private OffsetDateTime completedAt;

    @Builder.Default
    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
