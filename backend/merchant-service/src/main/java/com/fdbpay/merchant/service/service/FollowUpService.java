package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.Order;
import com.fdbpay.merchant.service.model.PaymentLink;
import com.fdbpay.merchant.service.model.enums.OrderStatus;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.OrderRepository;
import com.fdbpay.merchant.service.repository.PaymentLinkRepository;
import com.fdbpay.shared.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpService {

    private static final String NOTIFICATION_TOPIC = "notification.send";

    private final PaymentLinkRepository paymentLinkRepository;
    private final MerchantRepository merchantRepository;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void processFollowUps() {
        OffsetDateTime now = OffsetDateTime.now();
        List<PaymentLink> due = paymentLinkRepository.findDueForFollowUp(now);
        for (PaymentLink link : due) {
            try {
                sendReminder(link, now);
            } catch (Exception e) {
                log.warn("Auto follow-up failed for payment link {}: {}", link.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void processAbandonedOrders() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(6);
        List<Order> abandoned = findAbandoned(cutoff);
        for (Order order : abandoned) {
            try {
                sendAbandonedReminder(order);
            } catch (Exception e) {
                log.warn("Abandoned cart reminder failed for order {}: {}", order.getId(), e.getMessage());
            }
        }
        log.info("Abandoned cart scan complete, found {} candidates", abandoned.size());
    }

    private List<Order> findAbandoned(OffsetDateTime cutoff) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING
                        && o.getCustomerPhone() != null
                        && o.getCreatedAt() != null
                        && o.getCreatedAt().isBefore(cutoff))
                .toList();
    }

    private void sendAbandonedReminder(Order order) {
        Merchant merchant = merchantRepository.findById(order.getMerchantId()).orElse(null);
        String merchantName = merchant == null ? "Merchant" : merchant.getBusinessName();
        String body = String.format("You have an unfinished order of %d MMK at %s. Complete it now before it is cancelled.",
                order.getTotal(), merchantName);
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .userId(order.getMerchantId())
                    .channel("SMS")
                    .type("ABANDONED_CART")
                    .title("Complete your order")
                    .body(body)
                    .phone(order.getCustomerPhone())
                    .timestamp(OffsetDateTime.now())
                    .build();
            kafkaTemplate.send(NOTIFICATION_TOPIC, order.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to enqueue abandoned-cart SMS for order {}", order.getId(), e);
        }
    }

    private void sendReminder(PaymentLink link, OffsetDateTime now) {
        if (link.getReminderCount() >= 3) {
            link.setAutoFollowUp(false);
            paymentLinkRepository.save(link);
            return;
        }
        link.setReminderCount(link.getReminderCount() + 1);
        link.setNextReminderAt(now.plusHours(link.getFollowUpHours() == null ? 24 : link.getFollowUpHours()));
        paymentLinkRepository.save(link);

        Merchant merchant = merchantRepository.findById(link.getMerchantId()).orElse(null);
        String merchantName = merchant == null ? "Merchant" : merchant.getBusinessName();
        String body = String.format("Dear customer, you have a pending payment of %d MMK to %s. Link: https://pay.fdb.com.my/p/%s",
                link.getAmount(), merchantName, link.getToken());
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .userId(link.getMerchantId())
                    .channel("SMS")
                    .type("PAYMENT_LINK_REMINDER")
                    .title("Payment Reminder")
                    .body(body)
                    .phone(link.getCustomerPhone())
                    .timestamp(now)
                    .build();
            kafkaTemplate.send(NOTIFICATION_TOPIC, link.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to enqueue follow-up SMS for payment link {}", link.getId(), e);
        }
    }
}
