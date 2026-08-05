package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.ChargebackReason;
import com.fdbpay.merchant.service.model.enums.ChargebackStatus;
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
@Table(name = "chargebacks", indexes = {
        @Index(name = "idx_cb_merchant", columnList = "merchantId")
})
public class Chargeback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    private UUID transactionId;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "MMK";

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ChargebackReason reasonCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ChargebackStatus status = ChargebackStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String customerNotes;

    private OffsetDateTime deadline;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
