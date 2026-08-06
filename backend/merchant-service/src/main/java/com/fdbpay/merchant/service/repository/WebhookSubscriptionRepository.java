package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {
    List<WebhookSubscription> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
