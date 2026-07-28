package com.fdbpay.models.entity;

import com.fdbpay.models.enums.MerchantStatus;
import com.fdbpay.models.enums.SettlementType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String businessName;

    @Column(length = 50)
    private String businessType;

    @Column(length = 50)
    private String businessLicense;

    @Column(length = 30)
    private String taxId;

    @Column(length = 30)
    private String settlementAccount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private SettlementType settlementType = SettlementType.T1;

    @Column(length = 30)
    @Builder.Default
    private String feeSchedule = "STANDARD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MerchantStatus status = MerchantStatus.PENDING;

    @Column(length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(columnDefinition = "TEXT")
    private String qrStaticUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
