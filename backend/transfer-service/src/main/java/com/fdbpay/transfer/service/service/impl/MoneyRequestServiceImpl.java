package com.fdbpay.transfer.service.service.impl;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.event.NotificationEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.transfer.service.dto.request.CreateMoneyRequestRequest;
import com.fdbpay.transfer.service.dto.request.RespondMoneyRequestRequest;
import com.fdbpay.transfer.service.dto.request.TransferRequest;
import com.fdbpay.transfer.service.dto.response.MoneyRequestResponse;
import com.fdbpay.transfer.service.model.MoneyRequest;
import com.fdbpay.transfer.service.model.enums.MoneyRequestStatus;
import com.fdbpay.transfer.service.model.enums.TransactionType;
import com.fdbpay.transfer.service.repository.MoneyRequestRepository;
import com.fdbpay.transfer.service.service.MoneyRequestService;
import com.fdbpay.transfer.service.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoneyRequestServiceImpl implements MoneyRequestService {

    private final MoneyRequestRepository moneyRequestRepository;
    private final TransferService transferService;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    private static final String NOTIFICATION_TOPIC = "notification.send";

    @Override
    @Transactional
    public MoneyRequestResponse create(UUID userId, CreateMoneyRequestRequest request) {
        MoneyRequest moneyRequest = MoneyRequest.builder()
                .requesterUserId(userId)
                .targetPhone(request.getTargetPhone())
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(MoneyRequestStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .build();

        moneyRequest = moneyRequestRepository.save(moneyRequest);

        String paymentLink = "https://pay.fdbpay.com.mm/request/" + moneyRequest.getId();

        log.info("Money request created: id={}, requesterUserId={}, targetPhone={}, amount={}",
                moneyRequest.getId(), userId, request.getTargetPhone(), request.getAmount());

        sendMoneyRequestNotification(moneyRequest, paymentLink);

        return mapToResponse(moneyRequest, paymentLink);
    }

    @Override
    @Transactional
    public MoneyRequestResponse respond(UUID requestId, RespondMoneyRequestRequest response, UUID targetUserId) {
        MoneyRequest moneyRequest = moneyRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("MoneyRequest", requestId.toString()));

        if (moneyRequest.getStatus() != MoneyRequestStatus.PENDING) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Money request is not in PENDING status. Current status: " + moneyRequest.getStatus());
        }

        if (moneyRequest.getExpiresAt().isBefore(OffsetDateTime.now())) {
            moneyRequest.setStatus(MoneyRequestStatus.EXPIRED);
            moneyRequestRepository.save(moneyRequest);
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Money request has expired");
        }

        if (response.getAction() == RespondMoneyRequestRequest.Action.ACCEPT) {
            String idempotencyKey = "mr_accept_" + requestId + "_" + UUID.randomUUID();

            TransferRequest transferRequest = TransferRequest.builder()
                    .idempotencyKey(idempotencyKey)
                    .type(TransactionType.P2P)
                    .recipientIdentifier(moneyRequest.getRequesterUserId().toString())
                    .amount(moneyRequest.getAmount())
                    .description(moneyRequest.getDescription() != null ? moneyRequest.getDescription() : "Money request payment")
                    .build();

            var txnResponse = transferService.initiateTransfer(targetUserId, transferRequest);

            moneyRequest.setStatus(MoneyRequestStatus.ACCEPTED);
            moneyRequest.setPaymentId(txnResponse.getId());
            moneyRequest = moneyRequestRepository.save(moneyRequest);

            log.info("Money request accepted: id={}, paymentId={}", requestId, txnResponse.getId());
        } else {
            moneyRequest.setStatus(MoneyRequestStatus.CANCELLED);
            moneyRequest = moneyRequestRepository.save(moneyRequest);
            log.info("Money request cancelled by target: id={}", requestId);
        }

        String paymentLink = "https://pay.fdbpay.com.mm/request/" + moneyRequest.getId();
        return mapToResponse(moneyRequest, paymentLink);
    }

    @Override
    public Page<MoneyRequestResponse> getMyRequests(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<MoneyRequest> requests = moneyRequestRepository.findByRequesterUserIdOrderByCreatedAtDesc(userId, pageRequest);
        return requests.map(r -> mapToResponse(r, "https://pay.fdbpay.com.mm/request/" + r.getId()));
    }

    @Override
    public Page<MoneyRequestResponse> getByPhone(String phone, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<MoneyRequest> requests = moneyRequestRepository.findByTargetPhoneOrderByCreatedAtDesc(phone, pageRequest);
        return requests.map(r -> mapToResponse(r, "https://pay.fdbpay.com.mm/request/" + r.getId()));
    }

    private void sendMoneyRequestNotification(MoneyRequest moneyRequest, String paymentLink) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .userId(moneyRequest.getRequesterUserId())
                    .channel("SMS")
                    .type("MONEY_REQUEST")
                    .title("Money Request")
                    .body(String.format("You have a money request of %d MMK. Pay here: %s",
                            moneyRequest.getAmount(), paymentLink))
                    .phone(moneyRequest.getTargetPhone())
                    .timestamp(OffsetDateTime.now())
                    .build();
            kafkaTemplate.send(NOTIFICATION_TOPIC, moneyRequest.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to send money request notification: id={}", moneyRequest.getId(), e);
        }
    }

    private MoneyRequestResponse mapToResponse(MoneyRequest moneyRequest, String paymentLink) {
        return MoneyRequestResponse.builder()
                .id(moneyRequest.getId())
                .requesterUserId(moneyRequest.getRequesterUserId())
                .requesterName("User-" + moneyRequest.getRequesterUserId().toString().substring(0, 8))
                .targetPhone(moneyRequest.getTargetPhone())
                .amount(moneyRequest.getAmount())
                .description(moneyRequest.getDescription())
                .status(moneyRequest.getStatus())
                .paymentLink(paymentLink)
                .paymentId(moneyRequest.getPaymentId())
                .expiresAt(moneyRequest.getExpiresAt())
                .createdAt(moneyRequest.getCreatedAt())
                .build();
    }
}
