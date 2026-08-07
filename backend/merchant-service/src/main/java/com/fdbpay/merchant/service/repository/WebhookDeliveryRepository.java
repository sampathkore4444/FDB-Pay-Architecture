package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.WebhookDelivery;
import com.fdbpay.merchant.service.model.enums.WebhookDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
    Page<WebhookDelivery> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);

    List<WebhookDelivery> findByStatusAndNextRetryAtLessThanEqual(WebhookDeliveryStatus status, OffsetDateTime now);
}
