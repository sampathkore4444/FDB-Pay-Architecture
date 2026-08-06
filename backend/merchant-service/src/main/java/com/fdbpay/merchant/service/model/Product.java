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
@Table(name = "products", indexes = {
        @Index(name = "idx_prod_merchant", columnList = "merchantId")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(length = 255)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(length = 255)
    private String imageUrl;

    @Builder.Default
    @Column(nullable = false)
    private Long quantity = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long lowStockThreshold = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActiveStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
