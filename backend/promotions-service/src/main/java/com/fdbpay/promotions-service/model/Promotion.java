package com.fdbpay.promotions.service.model;

import com.fdbpay.promotions.service.model.enums.FundingType;
import com.fdbpay.promotions.service.model.enums.PromotionStatus;
import com.fdbpay.promotions.service.model.enums.PromotionType;
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
@Table(name = "promotions")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FundingType fundingType;

    private UUID merchantId;

    @Column(nullable = false)
    private Long discountValue;

    private Long maxDiscount;

    private Long minTransactionAmount;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxUsageTotal = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxUsagePerUser = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    @Column(nullable = false)
    private OffsetDateTime startDate;

    @Column(nullable = false)
    private OffsetDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PromotionStatus status = PromotionStatus.DRAFT;

    @Column(unique = true, length = 50)
    private String promoCode;

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
}
