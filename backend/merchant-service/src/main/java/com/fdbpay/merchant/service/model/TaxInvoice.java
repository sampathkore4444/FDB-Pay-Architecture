package com.fdbpay.merchant.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tax_invoices", indexes = {
        @Index(name = "idx_ti_merchant", columnList = "merchantId")
})
public class TaxInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, length = 40)
    private String invoiceNo;

    @Column(length = 150)
    private String customerName;

    @Column(length = 20)
    private String customerPhone;

    @Column(nullable = false)
    private Long subtotal;

    @Builder.Default
    @Column(nullable = false)
    private Long tax = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long withholdingTax = 0L;

    @Column(nullable = false)
    private Long total;

    @Column(nullable = false)
    private LocalDate issueDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
