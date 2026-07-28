package com.fdbpay.bill.service.service.impl;

import com.fdbpay.bill.service.dto.request.BillPaymentRequest;
import com.fdbpay.bill.service.dto.response.BillerResponse;
import com.fdbpay.bill.service.dto.response.BillLookupResponse;
import com.fdbpay.bill.service.dto.response.BillPaymentResponse;
import com.fdbpay.bill.service.model.BillPayment;
import com.fdbpay.bill.service.repository.BillPaymentRepository;
import com.fdbpay.bill.service.service.BillPaymentService;
import com.fdbpay.shared.constants.AppConstants;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.event.TransactionEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillPaymentServiceImpl implements BillPaymentService {

    private final BillPaymentRepository billPaymentRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String WALLET_SERVICE_BASE = "http://wallet-service/wallet";

    private static final List<BillerResponse> BILLERS = List.of(
            BillerResponse.builder()
                    .id(UUID.fromString("a0000000-0000-0000-0000-000000000001"))
                    .name("MEPCO")
                    .category("ELECTRICITY")
                    .description("Myanmar Electric Power Enterprise - Central")
                    .build(),
            BillerResponse.builder()
                    .id(UUID.fromString("a0000000-0000-0000-0000-000000000002"))
                    .name("SEPE")
                    .category("ELECTRICITY")
                    .description("State Electricity Power Enterprise")
                    .build(),
            BillerResponse.builder()
                    .id(UUID.fromString("a0000000-0000-0000-0000-000000000003"))
                    .name("ZP")
                    .category("ELECTRICITY")
                    .description("Yangon Electricity Supply Corporation")
                    .build(),
            BillerResponse.builder()
                    .id(UUID.fromString("b0000000-0000-0000-0000-000000000001"))
                    .name("Yangon Water")
                    .category("WATER")
                    .description("Yangon City Water Supply")
                    .build(),
            BillerResponse.builder()
                    .id(UUID.fromString("b0000000-0000-0000-0000-000000000002"))
                    .name("Mandalay Water")
                    .category("WATER")
                    .description("Mandalay City Water Supply")
                    .build(),
            BillerResponse.builder()
                    .id(UUID.fromString("c0000000-0000-0000-0000-000000000001"))
                    .name("MPT")
                    .category("INTERNET")
                    .description("Myanmar Posts and Telecommunications")
                    .build(),
            BillerResponse.builder()
                    .id(UUID.fromString("c0000000-0000-0000-0000-000000000002"))
                    .name("5BB")
                    .category("INTERNET")
                    .description("5BB Broadband")
                    .build(),
            BillerResponse.builder()
                    .id(UUID.fromString("c0000000-0000-0000-0000-000000000003"))
                    .name("GTV")
                    .category("INTERNET")
                    .description("GTV Internet Services")
                    .build(),
            BillerResponse.builder()
                    .id(UUID.fromString("d0000000-0000-0000-0000-000000000001"))
                    .name("MyTV")
                    .category("TV")
                    .description("MyTV Digital Television")
                    .build(),
            BillerResponse.builder()
                    .id(UUID.fromString("d0000000-0000-0000-0000-000000000002"))
                    .name("CANAL+")
                    .category("TV")
                    .description("CANAL+ Myanmar")
                    .build()
    );

    private static final Map<String, String> BILLER_NAMES = Map.of(
            "a0000000-0000-0000-0000-000000000001", "MEPCO",
            "a0000000-0000-0000-0000-000000000002", "SEPE",
            "a0000000-0000-0000-0000-000000000003", "ZP",
            "b0000000-0000-0000-0000-000000000001", "Yangon Water",
            "b0000000-0000-0000-0000-000000000002", "Mandalay Water",
            "c0000000-0000-0000-0000-000000000001", "MPT",
            "c0000000-0000-0000-0000-000000000002", "5BB",
            "c0000000-0000-0000-0000-000000000003", "GTV",
            "d0000000-0000-0000-0000-000000000001", "MyTV",
            "d0000000-0000-0000-0000-000000000002", "CANAL+"
    );

    private static final Set<String> CATEGORIES = Set.of("ELECTRICITY", "WATER", "INTERNET", "TV");

    @Override
    public List<BillerResponse> getCategories() {
        return CATEGORIES.stream()
                .map(cat -> BillerResponse.builder()
                        .name(cat)
                        .category(cat)
                        .build())
                .toList();
    }

    @Override
    public List<BillerResponse> getBillers(String category) {
        if (category == null || category.isBlank()) {
            return BILLERS;
        }
        return BILLERS.stream()
                .filter(b -> b.getCategory().equalsIgnoreCase(category))
                .toList();
    }

    @Override
    public BillLookupResponse lookupBill(UUID billerId, String accountNumber) {
        String billerName = BILLER_NAMES.get(billerId.toString());
        if (billerName == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Unknown biller ID: " + billerId);
        }

        long seed = accountNumber.hashCode() & 0xFFFFFFFFL;
        long simulatedAmount = 5000L + (seed % 500000L);

        return BillLookupResponse.builder()
                .accountNumber(accountNumber)
                .accountName("Account Holder - " + accountNumber.substring(Math.max(0, accountNumber.length() - 4)))
                .amountDue(simulatedAmount)
                .dueDate(OffsetDateTime.now().plusDays(30))
                .billerName(billerName)
                .period(OffsetDateTime.now().getMonth().toString() + " " + OffsetDateTime.now().getYear())
                .build();
    }

    @Override
    @Transactional
    public BillPaymentResponse payBill(UUID userId, BillPaymentRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        BillPayment billPayment = BillPayment.builder()
                .userId(userId)
                .billerId(request.getBillerId())
                .accountNumber(request.getAccountNumber())
                .amount(request.getAmount())
                .status(BillPayment.PaymentStatus.PENDING)
                .build();

        billPayment = billPaymentRepository.save(billPayment);
        log.info("Bill payment initiated: id={}, billerId={}, amount={}",
                billPayment.getId(), request.getBillerId(), request.getAmount());

        try {
            processBillPayment(billPayment);
        } catch (Exception e) {
            log.error("Bill payment processing failed: id={}", billPayment.getId(), e);
            billPayment.setStatus(BillPayment.PaymentStatus.FAILED);
            billPayment.setTransactionRef(e.getMessage());
            billPaymentRepository.save(billPayment);
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR, "Bill payment processing failed: " + e.getMessage());
        }

        return mapToResponse(billPayment);
    }

    @Override
    public Page<BillPaymentResponse> getHistory(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<BillPayment> payments = billPaymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);
        return payments.map(this::mapToResponse);
    }

    private void processBillPayment(BillPayment billPayment) {
        UUID txnId = billPayment.getId();
        String description = "Bill payment - " + BILLER_NAMES.getOrDefault(billPayment.getBillerId().toString(), "Unknown Biller");

        WebClient webClient = webClientBuilder.build();

        Map<String, Object> debitRequest = Map.of(
                "walletId", billPayment.getUserId().toString(),
                "amount", billPayment.getAmount(),
                "description", description,
                "txnId", txnId.toString()
        );

        webClient.post()
                .uri(WALLET_SERVICE_BASE + "/debit")
                .bodyValue(debitRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        billPayment.setStatus(BillPayment.PaymentStatus.COMPLETED);
        billPayment.setTransactionRef(txnId.toString());
        billPaymentRepository.save(billPayment);

        TransactionEvent event = TransactionEvent.builder()
                .transactionId(txnId)
                .type("BILL_PAY")
                .status("COMPLETED")
                .amount(billPayment.getAmount())
                .currency("MMK")
                .senderUserId(billPayment.getUserId())
                .timestamp(OffsetDateTime.now())
                .metadata(Map.of("billerId", billPayment.getBillerId().toString(),
                        "accountNumber", billPayment.getAccountNumber()))
                .build();

        try {
            kafkaTemplate.send(AppConstants.TOPIC_TXN_COMPLETED, txnId.toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish bill payment event: id={}", txnId, e);
        }

        log.info("Bill payment completed: id={}, billerId={}, amount={}",
                txnId, billPayment.getBillerId(), billPayment.getAmount());
    }

    private void checkIdempotency(String idempotencyKey) {
        String cacheKey = AppConstants.IDEMPOTENCY_CACHE_PREFIX + idempotencyKey;
        Boolean exists = redisTemplate.hasKey(cacheKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BusinessException(ErrorCodes.DUPLICATE_TRANSACTION,
                    "Duplicate bill payment detected for idempotency key: " + idempotencyKey);
        }
        redisTemplate.opsForValue().set(cacheKey, "1", 24, TimeUnit.HOURS);
    }

    private BillPaymentResponse mapToResponse(BillPayment billPayment) {
        return BillPaymentResponse.builder()
                .id(billPayment.getId())
                .userId(billPayment.getUserId())
                .billerId(billPayment.getBillerId())
                .accountNumber(billPayment.getAccountNumber())
                .amount(billPayment.getAmount())
                .transactionRef(billPayment.getTransactionRef())
                .status(billPayment.getStatus())
                .createdAt(billPayment.getCreatedAt())
                .build();
    }
}
