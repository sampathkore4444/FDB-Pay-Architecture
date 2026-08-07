package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.WebhookSubscriptionRequest;
import com.fdbpay.merchant.service.dto.response.WebhookDeliveryResponse;
import com.fdbpay.merchant.service.dto.response.WebhookSubscriptionResponse;
import com.fdbpay.merchant.service.model.WebhookDelivery;
import com.fdbpay.merchant.service.model.WebhookSubscription;
import com.fdbpay.merchant.service.model.enums.WebhookDeliveryStatus;
import com.fdbpay.merchant.service.model.enums.WebhookEvent;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.WebhookDeliveryRepository;
import com.fdbpay.merchant.service.repository.WebhookSubscriptionRepository;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final MerchantRepository merchantRepository;
    private final AuditService auditService;

    public List<WebhookSubscriptionResponse> listSubscriptions(UUID merchantId) {
        requireMerchant(merchantId);
        return subscriptionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream().map(this::mapSubscription).toList();
    }

    @Transactional
    public WebhookSubscriptionResponse createSubscription(UUID merchantId, WebhookSubscriptionRequest request) {
        requireMerchant(merchantId);
        WebhookEvent event = parseEvent(request.getEvent());
        subscriptionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .filter(s -> s.getEvent() == event)
                .findFirst()
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "A subscription already exists for event " + event);
                });
        WebhookSubscription subscription = WebhookSubscription.builder()
                .merchantId(merchantId)
                .event(event)
                .url(request.getUrl())
                .secret("whsec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .enabled(true)
                .maxRetries(request.getMaxRetries() == null ? 3 : request.getMaxRetries())
                .backoffMinutes(request.getBackoffMinutes() == null ? 5 : request.getBackoffMinutes())
                .build();
        subscription = subscriptionRepository.save(subscription);
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "WEBHOOK", subscription.getId().toString(),
                "Subscribed to webhook event " + event + " -> " + subscription.getUrl());
        return mapSubscription(subscription);
    }

    @Transactional
    public WebhookSubscriptionResponse toggleSubscription(UUID merchantId, UUID subscriptionId) {
        WebhookSubscription subscription = getOwned(merchantId, subscriptionId);
        subscription.setEnabled(!subscription.isEnabled());
        subscription = subscriptionRepository.save(subscription);
        return mapSubscription(subscription);
    }

    @Transactional
    public void deleteSubscription(UUID merchantId, UUID subscriptionId) {
        WebhookSubscription subscription = getOwned(merchantId, subscriptionId);
        subscriptionRepository.delete(subscription);
        auditService.log(merchantId, "OWNER", null, null, "DELETE", "WEBHOOK", subscriptionId.toString(),
                "Removed webhook subscription for " + subscription.getEvent());
    }

    @Transactional
    public WebhookDeliveryResponse sendTest(UUID merchantId, UUID subscriptionId) {
        WebhookSubscription subscription = getOwned(merchantId, subscriptionId);
        String payload = "{\"type\":\"" + subscription.getEvent() + "\",\"merchantId\":\"" + merchantId + "\",\"test\":true,\"timestamp\":\"" + OffsetDateTime.now() + "\"}";
        return deliver(merchantId, subscription, payload);
    }

    @Transactional
    public WebhookDeliveryResponse replay(UUID merchantId, UUID deliveryId) {
        WebhookDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookDelivery", deliveryId.toString()));
        if (!delivery.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Delivery does not belong to this merchant");
        }
        WebhookSubscription subscription = subscriptionRepository.findById(delivery.getSubscriptionId()).orElse(null);
        if (subscription == null || !subscription.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Original subscription no longer exists");
        }
        return deliver(merchantId, subscription, delivery.getPayload());
    }

    public Page<WebhookDeliveryResponse> listDeliveries(UUID merchantId, int page, int size) {
        requireMerchant(merchantId);
        return deliveryRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(page, size))
                .map(this::mapDelivery);
    }

    private WebhookDeliveryResponse deliver(UUID merchantId, WebhookSubscription subscription, String payload) {
        int attempts = 0;
        Integer statusCode = null;
        String error = null;
        boolean success = false;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.USER_AGENT, "fdbpay-webhooks/1.0");
            headers.set("X-Webhook-Signature", sign(payload, subscription.getSecret()));
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.exchange(subscription.getUrl(), HttpMethod.POST, entity, String.class);
            attempts = 1;
            statusCode = response.getStatusCode().value();
            success = statusCode < 300;
        } catch (Exception e) {
            attempts = 1;
            error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage().substring(0, Math.min(200, e.getMessage().length()));
            log.warn("Webhook delivery failed to {}: {}", subscription.getUrl(), e.getMessage());
        }
        WebhookDelivery delivery = WebhookDelivery.builder()
                .merchantId(merchantId)
                .subscriptionId(subscription.getId())
                .event(subscription.getEvent())
                .url(subscription.getUrl())
                .payload(payload)
                .status(success ? WebhookDeliveryStatus.SUCCESS : WebhookDeliveryStatus.FAILED)
                .attempts(attempts)
                .retryCount(0)
                .statusCode(statusCode)
                .error(error)
                .deliveredAt(OffsetDateTime.now())
                .build();
        if (!success) {
            scheduleRetry(delivery, subscription);
        }
        delivery = deliveryRepository.save(delivery);
        return mapDelivery(delivery);
    }

    private void scheduleRetry(WebhookDelivery delivery, WebhookSubscription subscription) {
        int maxRetries = subscription.getMaxRetries() == null ? 3 : subscription.getMaxRetries();
        if (maxRetries <= 0) {
            return;
        }
        int backoff = subscription.getBackoffMinutes() == null ? 5 : subscription.getBackoffMinutes();
        delivery.setRetryCount(1);
        delivery.setNextRetryAt(OffsetDateTime.now().plusMinutes(backoff));
        delivery.setStatus(WebhookDeliveryStatus.FAILED);
    }

    @Scheduled(cron = "0 */2 * * * *")
    @Transactional
    public void processRetries() {
        List<WebhookDelivery> due = deliveryRepository.findByStatusAndNextRetryAtLessThanEqual(WebhookDeliveryStatus.FAILED, OffsetDateTime.now());
        for (WebhookDelivery delivery : due) {
            WebhookSubscription subscription = delivery.getSubscriptionId() == null ? null
                    : subscriptionRepository.findById(delivery.getSubscriptionId()).orElse(null);
            if (subscription == null || !subscription.isEnabled()) {
                delivery.setStatus(WebhookDeliveryStatus.FAILED);
                delivery.setNextRetryAt(null);
                deliveryRepository.save(delivery);
                continue;
            }
            int maxRetries = subscription.getMaxRetries() == null ? 3 : subscription.getMaxRetries();
            int current = delivery.getRetryCount() == null ? 0 : delivery.getRetryCount();
            boolean success = attemptDelivery(delivery, subscription);
            if (success) {
                delivery.setStatus(WebhookDeliveryStatus.SUCCESS);
                delivery.setNextRetryAt(null);
                delivery.setError(null);
                delivery.setDeliveredAt(OffsetDateTime.now());
            } else if (current >= maxRetries) {
                delivery.setNextRetryAt(null);
            } else {
                int backoff = subscription.getBackoffMinutes() == null ? 5 : subscription.getBackoffMinutes();
                delivery.setNextRetryAt(OffsetDateTime.now().plusMinutes(backoff * (long) current));
                delivery.setRetryCount(current + 1);
            }
            delivery.setAttempts((delivery.getAttempts() == null ? 0 : delivery.getAttempts()) + 1);
            deliveryRepository.save(delivery);
        }
    }

    private boolean attemptDelivery(WebhookDelivery delivery, WebhookSubscription subscription) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.USER_AGENT, "fdbpay-webhooks/1.0");
            headers.set("X-Webhook-Signature", sign(delivery.getPayload(), subscription.getSecret()));
            HttpEntity<String> entity = new HttpEntity<>(delivery.getPayload(), headers);
            ResponseEntity<String> response = restTemplate.exchange(delivery.getUrl(), HttpMethod.POST, entity, String.class);
            delivery.setStatusCode(response.getStatusCode().value());
            return response.getStatusCode().value() < 300;
        } catch (Exception e) {
            delivery.setError(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage().substring(0, Math.min(200, e.getMessage().length())));
            log.warn("Webhook retry failed to {}: {}", delivery.getUrl(), e.getMessage());
            return false;
        }
    }

    private String sign(String payload, String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((payload + secret).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private WebhookEvent parseEvent(String raw) {
        try {
            return WebhookEvent.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Unsupported webhook event: " + raw);
        }
    }

    private WebhookSubscription getOwned(UUID merchantId, UUID subscriptionId) {
        WebhookSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookSubscription", subscriptionId.toString()));
        if (!subscription.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Subscription does not belong to this merchant");
        }
        return subscription;
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private WebhookSubscriptionResponse mapSubscription(WebhookSubscription subscription) {
        return WebhookSubscriptionResponse.builder()
                .id(subscription.getId())
                .event(subscription.getEvent())
                .url(subscription.getUrl())
                .enabled(subscription.isEnabled())
                .maxRetries(subscription.getMaxRetries())
                .backoffMinutes(subscription.getBackoffMinutes())
                .createdAt(subscription.getCreatedAt())
                .build();
    }

    private WebhookDeliveryResponse mapDelivery(WebhookDelivery delivery) {
        return WebhookDeliveryResponse.builder()
                .id(delivery.getId())
                .subscriptionId(delivery.getSubscriptionId())
                .event(delivery.getEvent())
                .url(delivery.getUrl())
                .payload(delivery.getPayload())
                .status(delivery.getStatus())
                .attempts(delivery.getAttempts())
                .retryCount(delivery.getRetryCount())
                .nextRetryAt(delivery.getNextRetryAt())
                .statusCode(delivery.getStatusCode())
                .error(delivery.getError())
                .createdAt(delivery.getCreatedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .build();
    }
}
