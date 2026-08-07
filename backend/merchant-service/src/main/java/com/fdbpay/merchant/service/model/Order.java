package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_merchant", columnList = "merchantId"),
        @Index(name = "idx_orders_customer", columnList = "customerPhone"),
        @Index(name = "idx_orders_status", columnList = "status")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    private UUID storeId;

    @Column(length = 20)
    private String customerPhone;

    @Column(length = 150)
    private String customerName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String items;

    @Column(nullable = false)
    private Long subtotal;

    @Builder.Default
    @Column(nullable = false)
    private Long tax = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Integer taxRate = 0;

    @Column(nullable = false)
    private Long total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private Long refundAmount = 0L;

    private OffsetDateTime paidAt;

    private OffsetDateTime fulfilledAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
