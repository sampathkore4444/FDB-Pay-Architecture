package com.fdbpay.promotions.service.model;

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
@Table(name = "promotion_usages")
public class PromotionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID promotionId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID transactionId;

    @Column(nullable = false)
    @Builder.Default
    private Long discountApplied = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long cashbackAmount = 0L;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
