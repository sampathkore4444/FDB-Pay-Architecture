package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.PaymentLinkStatus;
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
@Table(name = "payment_links", indexes = {
        @Index(name = "idx_payment_link_merchant", columnList = "merchantId"),
        @Index(name = "idx_payment_link_token", columnList = "token", unique = true),
        @Index(name = "idx_payment_link_status", columnList = "status")
})
public class PaymentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false)
    private Long amount;

    private String description;

    @Column(length = 20)
    private String customerPhone;

    @Column(length = 200)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentLinkStatus status = PaymentLinkStatus.ACTIVE;

    @Builder.Default
    @Column(nullable = false)
    private boolean singleUse = true;

    private OffsetDateTime paidAt;

    private OffsetDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private Integer reminderCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean autoFollowUp = false;

    @Builder.Default
    @Column(nullable = false)
    private Integer followUpHours = 24;

    private OffsetDateTime nextReminderAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
