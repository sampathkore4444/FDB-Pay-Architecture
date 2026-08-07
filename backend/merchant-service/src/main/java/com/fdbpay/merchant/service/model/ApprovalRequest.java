package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.ApprovalStatus;
import com.fdbpay.merchant.service.model.enums.ApprovalType;
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
@Table(name = "approval_requests", indexes = {
        @Index(name = "idx_ar_merchant", columnList = "merchantId, status")
})
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalType type;

    @Column(nullable = false)
    private Long amount;

    private UUID refId;

    private UUID initiatorId;

    @Column(length = 150)
    private String initiatorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(length = 150)
    private String reviewedBy;

    private OffsetDateTime reviewedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
