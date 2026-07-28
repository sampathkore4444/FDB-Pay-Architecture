package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.MerchantStatus;
import com.fdbpay.merchant.service.model.enums.SettlementType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "merchants", indexes = {
        @Index(name = "idx_merchant_user_id", columnList = "userId"),
        @Index(name = "idx_merchant_license", columnList = "businessLicense"),
        @Index(name = "idx_merchant_tax_id", columnList = "taxId"),
        @Index(name = "idx_merchant_status", columnList = "status")
})
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String businessName;

    private String businessType;

    private String businessLicense;

    private String taxId;

    private String settlementAccount;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private SettlementType settlementType;

    private String feeSchedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MerchantStatus status = MerchantStatus.PENDING;

    private String category;

    private String address;

    private Double latitude;

    private Double longitude;

    private String qrStaticUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}
