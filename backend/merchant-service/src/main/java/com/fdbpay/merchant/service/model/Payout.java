package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.PayoutStatus;
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
@Table(name = "payouts", indexes = {
        @Index(name = "idx_payout_merchant", columnList = "merchantId")
})
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false, length = 150)
    private String accountLabel;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutStatus status;

    @Column(length = 100)
    private String reference;

    @Column(length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime completedAt;
}
