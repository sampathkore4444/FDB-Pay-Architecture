package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.TaxInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaxInvoiceRepository extends JpaRepository<TaxInvoice, UUID> {

    List<TaxInvoice> findByMerchantIdOrderByIssueDateDesc(UUID merchantId);

    List<TaxInvoice> findByMerchantIdAndIssueDateBetweenOrderByIssueDateDesc(UUID merchantId, LocalDate from, LocalDate to);

    @Query("select coalesce(sum(t.tax), 0) from TaxInvoice t where t.merchantId = :merchantId and t.issueDate between :from and :to")
    Long sumTax(@Param("merchantId") UUID merchantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select coalesce(sum(t.withholdingTax), 0) from TaxInvoice t where t.merchantId = :merchantId and t.issueDate between :from and :to")
    Long sumWithholding(@Param("merchantId") UUID merchantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select coalesce(sum(t.total), 0) from TaxInvoice t where t.merchantId = :merchantId and t.issueDate between :from and :to")
    Long sumTotal(@Param("merchantId") UUID merchantId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
