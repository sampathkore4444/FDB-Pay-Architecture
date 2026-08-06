package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.ActiveStatus;
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
@Table(name = "cashback_campaigns", indexes = {
        @Index(name = "idx_cc_merchant", columnList = "merchantId")
})
public class CashbackCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer percent;

    @Column(nullable = false)
    private Long budget;

    @Builder.Default
    @Column(nullable = false)
    private Long spent = 0L;

    private OffsetDateTime startsAt;

    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActiveStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
