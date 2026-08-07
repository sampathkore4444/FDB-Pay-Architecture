package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.DigitalDeliveryStatus;
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
@Table(name = "digital_deliveries", indexes = {
        @Index(name = "idx_dd_order", columnList = "orderId")
})
public class DigitalDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    private UUID orderId;

    private UUID productId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 20)
    private String deliveredTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DigitalDeliveryStatus status = DigitalDeliveryStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime deliveredAt;
}
