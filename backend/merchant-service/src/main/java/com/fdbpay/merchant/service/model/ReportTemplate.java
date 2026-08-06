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
@Table(name = "report_templates", indexes = {
        @Index(name = "idx_rt_merchant", columnList = "merchantId")
})
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String reportType;

    @Column(nullable = false, length = 20)
    private String frequency;

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String format = "CSV";

    @Column(length = 150)
    private String email;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
