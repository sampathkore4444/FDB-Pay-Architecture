package com.fdbpay.remittance.service.model;

import com.fdbpay.remittance.service.model.enums.RemittanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "remittances")
public class Remittance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID recipientUserId;

    @Column(nullable = false, length = 20)
    private String recipientPhone;

    @Column(nullable = false, length = 100)
    private String senderName;

    @Column(nullable = false, length = 5)
    private String senderCountry;

    @Column(nullable = false, length = 10)
    private String corridor;

    @Column(nullable = false, length = 100)
    private String partnerRef;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long fee;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Column(nullable = false)
    private Long amountMmk;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RemittanceStatus status = RemittanceStatus.PENDING;

    @Column(nullable = false, unique = true, length = 50)
    private String referenceNumber;

    private OffsetDateTime receivedAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
