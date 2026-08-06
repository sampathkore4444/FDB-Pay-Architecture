package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, UUID> {
    List<ReportTemplate> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
