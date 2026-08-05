package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.FinancingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "financing_applications", indexes = {
        @Index(name = "idx_fa_merchant", columnList = "merchantId")
})
public class FinancingApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private Long requestedAmount;

    @Column(nullable = false)
    private Integer termMonths;

    @Column(length = 100)
    private String purpose;

    @Column(nullable = false)
    @Builder.Default
    private Long monthlyRevenue = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long estimatedLimit = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FinancingStatus status = FinancingStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime reviewedAt;
}
