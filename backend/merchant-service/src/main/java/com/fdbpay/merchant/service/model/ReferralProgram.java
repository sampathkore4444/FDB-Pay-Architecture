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
@Table(name = "referral_programs", indexes = {
        @Index(name = "idx_ref_merchant", columnList = "merchantId")
})
public class ReferralProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, length = 30)
    private String code;

    @Builder.Default
    @Column(nullable = false)
    private Long referralBonus = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long referredBonus = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Integer uses = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActiveStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
