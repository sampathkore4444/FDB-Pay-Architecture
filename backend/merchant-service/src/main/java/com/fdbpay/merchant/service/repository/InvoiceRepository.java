package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.Invoice;
import com.fdbpay.merchant.service.model.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);

    List<Invoice> findByStatus(InvoiceStatus status);

    List<Invoice> findByMerchantIdAndStatus(UUID merchantId, InvoiceStatus status);
}
