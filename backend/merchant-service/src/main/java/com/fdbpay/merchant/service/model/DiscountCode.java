package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import com.fdbpay.merchant.service.model.enums.DiscountType;
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
@Table(name = "discount_codes", indexes = {
        @Index(name = "idx_dc_merchant", columnList = "merchantId")
})
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType type;

    @Column(nullable = false)
    private Long value;

    @Builder.Default
    @Column(nullable = false)
    private Long minSpend = 0L;

    private Integer maxUses;

    @Builder.Default
    @Column(nullable = false)
    private Integer usedCount = 0;

    private OffsetDateTime validFrom;

    private OffsetDateTime validTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActiveStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
