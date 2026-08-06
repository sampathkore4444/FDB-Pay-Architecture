package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.FraudRuleType;
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
@Table(name = "fraud_rules", indexes = {
        @Index(name = "idx_fraud_merchant", columnList = "merchantId")
})
public class FraudRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FraudRuleType ruleType;

    @Column(nullable = false)
    private Long threshold;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
