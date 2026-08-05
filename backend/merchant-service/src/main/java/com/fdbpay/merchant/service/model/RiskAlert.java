package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.RiskAlertStatus;
import com.fdbpay.merchant.service.model.enums.RiskSeverity;
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
@Table(name = "risk_alerts", indexes = {
        @Index(name = "idx_ra_merchant", columnList = "merchantId")
})
public class RiskAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(length = 50)
    private String alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private RiskSeverity severity = RiskSeverity.MEDIUM;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RiskAlertStatus status = RiskAlertStatus.OPEN;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime acknowledgedAt;
}
