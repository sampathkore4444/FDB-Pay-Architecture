package com.fdbpay.promotions.service.consumer;

import com.fdbpay.shared.event.TransactionEvent;
import com.fdbpay.shared.event.NotificationEvent;
import com.fdbpay.promotions.service.model.Promotion;
import com.fdbpay.promotions.service.model.enums.PromotionStatus;
import com.fdbpay.promotions.service.model.enums.PromotionType;
import com.fdbpay.promotions.service.repository.PromotionRepository;
import com.fdbpay.promotions.service.service.PromotionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionEventConsumer {

    private final PromotionRepository promotionRepository;
    private final PromotionsService promotionsService;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @KafkaListener(topics = "txn.completed", groupId = "promotions-service")
    public void handleTransactionCompleted(TransactionEvent event) {
        log.info("Processing transaction for cashback eligibility: txnId={}, amount={}, receiverUserId={}",
                event.getTransactionId(), event.getAmount(), event.getReceiverUserId());

        if (event.getReceiverUserId() == null || event.getAmount() == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        List<Promotion> cashbackPromotions = promotionRepository
                .findByStatusAndStartDateBeforeAndEndDateAfter(PromotionStatus.ACTIVE, now, now)
                .stream()
                .filter(p -> p.getType() == PromotionType.CASHBACK)
                .toList();

        for (Promotion promotion : cashbackPromotions) {
            if (promotion.getMinTransactionAmount() != null && event.getAmount() < promotion.getMinTransactionAmount()) {
                continue;
            }

            try {
                promotionsService.applyPromotion(
                        event.getReceiverUserId(),
                        new com.fdbpay.promotions.service.dto.request.ApplyPromotionRequest(
                                promotion.getPromoCode(), event.getAmount()),
                        event.getTransactionId());
                log.info("Auto-applied cashback promotion: promotionId={}, userId={}, txnId={}",
                        promotion.getId(), event.getReceiverUserId(), event.getTransactionId());
            } catch (Exception e) {
                log.warn("Failed to auto-apply cashback: promotionId={}, userId={}, error={}",
                        promotion.getId(), event.getReceiverUserId(), e.getMessage());
            }
        }
    }
}
