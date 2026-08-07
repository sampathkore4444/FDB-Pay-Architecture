package com.fdbpay.merchant.service.model;

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
@Table(name = "product_variants", indexes = {
        @Index(name = "idx_pv_product", columnList = "productId")
})
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 80)
    private String sku;

    @Column(length = 150)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private Long priceDelta = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long quantity = 0L;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
