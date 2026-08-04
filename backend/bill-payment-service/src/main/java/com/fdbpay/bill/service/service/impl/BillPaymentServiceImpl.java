package com.fdbpay.bill.service.service.impl;

import com.fdbpay.bill.service.dto.refdata.RefDataLookup;
import com.fdbpay.bill.service.dto.refdata.RefDataType;
import com.fdbpay.bill.service.dto.refdata.RefDataTypePage;
import com.fdbpay.bill.service.dto.refdata.RefDataValue;
import com.fdbpay.bill.service.dto.request.BillPaymentRequest;
import com.fdbpay.bill.service.dto.response.BillerResponse;
import com.fdbpay.bill.service.dto.response.BillLookupResponse;
import com.fdbpay.bill.service.dto.response.BillPaymentResponse;
import com.fdbpay.bill.service.model.BillPayment;
import com.fdbpay.bill.service.repository.BillPaymentRepository;
import com.fdbpay.bill.service.service.BillPaymentService;
import com.fdbpay.shared.constants.AppConstants;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.shared.event.TransactionEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
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
    private static final String REFERENCE_DATA_SERVICE_BASE = "http://reference-data-service/refdata";
    private static final Set<String> EXCLUDED_CATEGORY_CODES = Set.of("AIRTIME", "BILLER");

    @Override
    public List<BillerResponse> getCategories() {
        try {
            ApiResponse<RefDataTypePage> resp = webClientBuilder.build()
                    .get()
                    .uri(REFERENCE_DATA_SERVICE_BASE + "/types?page=0&size=100")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<RefDataTypePage>>() {})
                    .block();
            if (resp == null || !resp.isSuccess() || resp.getData() == null || resp.getData().getContent() == null) {
                return List.of();
            }
            return resp.getData().getContent().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getActive()))
                    .filter(t -> !EXCLUDED_CATEGORY_CODES.contains(t.getCode()))
                    .map(t -> BillerResponse.builder()
                            .name(t.getCode())
                            .category(t.getCode())
                            .description(t.getDescription())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to load bill categories from reference data: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<BillerResponse> getBillers(String category) {
        if (category == null || category.isBlank()) {
            return List.of();
        }
        try {
            ApiResponse<RefDataLookup> resp = webClientBuilder.build()
                    .get()
                    .uri(REFERENCE_DATA_SERVICE_BASE + "/type/" + category)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<RefDataLookup>>() {})
                    .block();
            if (resp == null || !resp.isSuccess() || resp.getData() == null || resp.getData().getValues() == null) {
                return List.of();
            }
            return resp.getData().getValues().stream()
                    .map(v -> BillerResponse.builder()
                            .id(v.getId())
                            .name(v.getValue())
                            .category(category)
                            .description(v.getCode())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to load billers for category {} from reference data: {}", category, e.getMessage());
            return List.of();
        }
    }

    @Override
    public BillLookupResponse lookupBill(UUID billerId, String accountNumber) {
        String billerName = resolveBillerName(billerId);

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
        String description = "Bill payment - " + resolveBillerName(billPayment.getBillerId());

        WebClient webClient = webClientBuilder.build();
        UUID walletId = getWalletIdByUserId(billPayment.getUserId());

        Map<String, Object> debitRequest = Map.of(
                "walletId", walletId.toString(),
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

    private UUID getWalletIdByUserId(UUID userId) {
        Map<?, ?> response = webClientBuilder.build()
                .get()
                .uri(WALLET_SERVICE_BASE + "?userId=" + userId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "User wallet could not be resolved");
        }
        Object data = response.get("data");
        Object walletId = data != null ? ((Map<?, ?>) data).get("id") : null;
        if (walletId == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "User wallet could not be resolved");
        }
        return UUID.fromString(walletId.toString());
    }

    private String resolveBillerName(UUID billerId) {
        try {
            ApiResponse<RefDataValue> resp = webClientBuilder.build()
                    .get()
                    .uri(REFERENCE_DATA_SERVICE_BASE + "/values/" + billerId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<RefDataValue>>() {})
                    .block();
            if (resp != null && resp.isSuccess() && resp.getData() != null) {
                return resp.getData().getValue();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve biller name for {} from reference data: {}", billerId, e.getMessage());
        }
        return "Unknown Biller";
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
