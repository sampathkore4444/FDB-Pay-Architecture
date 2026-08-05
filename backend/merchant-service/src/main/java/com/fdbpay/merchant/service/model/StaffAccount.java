package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.StaffAccountStatus;
import com.fdbpay.merchant.service.model.enums.StaffRole;
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
@Table(name = "staff_accounts", indexes = {
        @Index(name = "idx_sa_merchant_id", columnList = "merchantId"),
        @Index(name = "idx_sa_user_id", columnList = "userId")
})
public class StaffAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StaffAccountStatus status = StaffAccountStatus.ACTIVE;

    private Long dailyLimit;

    private UUID storeId;

    @Column(columnDefinition = "TEXT")
    private String permissions;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
