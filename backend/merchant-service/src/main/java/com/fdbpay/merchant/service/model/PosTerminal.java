package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.PosTerminalStatus;
import com.fdbpay.merchant.service.model.enums.PosTerminalType;
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
@Table(name = "pos_terminals", indexes = {
        @Index(name = "idx_pt_merchant_id", columnList = "merchantId"),
        @Index(name = "idx_pt_serial", columnList = "serialNumber", unique = true)
})
public class PosTerminal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, unique = true)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PosTerminalType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PosTerminalStatus status = PosTerminalStatus.ACTIVE;

    private OffsetDateTime lastPingAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
