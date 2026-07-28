package com.fdbpay.remittance.service.model;

import com.fdbpay.remittance.service.model.enums.RemittanceCorridorStatus;
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
@Table(name = "remittance_corridors")
public class RemittanceCorridor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 5)
    private String sourceCountry;

    @Column(nullable = false, length = 3)
    private String sourceCurrency;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String destCurrency = "MMK";

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Column(nullable = false)
    private Long feeFixed;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal feePercentage;

    @Column(nullable = false)
    private Long minAmount;

    @Column(nullable = false)
    private Long maxAmount;

    @Column(nullable = false, length = 100)
    private String partnerName;

    @Column(length = 500)
    private String partnerApiUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RemittanceCorridorStatus status = RemittanceCorridorStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
