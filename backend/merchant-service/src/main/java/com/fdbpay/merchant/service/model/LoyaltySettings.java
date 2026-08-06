package com.fdbpay.merchant.service.model;

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
@Table(name = "loyalty_settings", indexes = {
        @Index(name = "idx_loyalty_merchant", columnList = "merchantId")
})
public class LoyaltySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Builder.Default
    @Column(nullable = false)
    private Integer pointsPerMmk = 1;

    @Builder.Default
    @Column(nullable = false)
    private Integer rewardThresholdPoints = 1000;

    @Builder.Default
    @Column(nullable = false)
    private Long rewardValue = 1000L;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
