package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.RecurringInterval;
import com.fdbpay.merchant.service.model.enums.RecurringPlanStatus;
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
@Table(name = "recurring_plans", indexes = {
        @Index(name = "idx_rp_merchant", columnList = "merchantId")
})
public class RecurringPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 20)
    private String customerPhone;

    @Column(length = 150)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringInterval interval;

    private Integer dayOfWeek;

    private Integer dayOfMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringPlanStatus status;

    private Integer maxCharges;

    @Builder.Default
    @Column(nullable = false)
    private Integer chargeCount = 0;

    private OffsetDateTime nextRunAt;

    private OffsetDateTime lastChargeAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
