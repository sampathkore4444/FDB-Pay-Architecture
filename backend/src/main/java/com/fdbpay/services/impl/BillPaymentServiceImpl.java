package com.fdbpay.services.impl;

import com.fdbpay.common.constants.ErrorCodes;
import com.fdbpay.common.exceptions.BusinessException;
import com.fdbpay.common.exceptions.ResourceNotFoundException;
import com.fdbpay.common.utils.IdGenerator;
import com.fdbpay.models.entity.Transaction;
import com.fdbpay.models.entity.Wallet;
import com.fdbpay.models.enums.TransactionStatus;
import com.fdbpay.models.enums.TransactionType;
import com.fdbpay.repositories.TransactionRepository;
import com.fdbpay.repositories.WalletRepository;
import com.fdbpay.schemas.request.BillPaymentRequest;
import com.fdbpay.schemas.response.TransactionResponse;
import com.fdbpay.services.BillPaymentService;
import com.fdbpay.services.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillPaymentServiceImpl implements BillPaymentService {

    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    private static final Map<String, List<Map<String, String>>> BILLERS = new ConcurrentHashMap<>();

    static {
        BILLERS.put("ELECTRICITY", List.of(
                Map.of("id", "MEPCO", "name", "Myanmar Electric Power Corporation"),
                Map.of("id", "SEPE", "name", "South East Power Enterprise"),
                Map.of("id", "ZP", "name", "Zayyabar Power"),
                Map.of("id", "EP", "name", "Edition Power"),
                Map.of("id", "CBE", "name", "Circle Power")
        ));
        BILLERS.put("WATER", List.of(
                Map.of("id", "YANGON_WATER", "name", "Yangon City Water Supply"),
                Map.of("id", "MANDALAY_WATER", "name", "Mandalay City Water Supply")
        ));
        BILLERS.put("INTERNET", List.of(
                Map.of("id", "MPT", "name", "Myanma Posts & Telecom"),
                Map.of("id", "5BB", "name", "5BB Broadband"),
                Map.of("id", "GTV", "name", "GTV Internet")
        ));
        BILLERS.put("TV", List.of(
                Map.of("id", "MYTV", "name", "MyTV"),
                Map.of("id", "CANAL", "name", "CANAL+")
        ));
        BILLERS.put("MOBILE_POSTPAID", List.of(
                Map.of("id", "MPT_POSTPAID", "name", "MPT Postpaid"),
                Map.of("id", "OOREDOO_POSTPAID", "name", "Ooredoo Postpaid")
        ));
    }

    @Override
    public List<Map<String, String>> getCategories() {
        return List.of(
                Map.of("id", "ELECTRICITY", "name", "Electricity"),
                Map.of("id", "WATER", "name", "Water"),
                Map.of("id", "INTERNET", "name", "Internet"),
                Map.of("id", "TV", "name", "TV"),
                Map.of("id", "MOBILE_POSTPAID", "name", "Mobile Postpaid")
        );
    }

    @Override
    public List<Map<String, String>> getBillers(String category) {
        return BILLERS.getOrDefault(category.toUpperCase(), List.of());
    }

    @Override
    public Map<String, Object> lookupBill(String billerId, String accountNumber) {
        return Map.of(
                "billerId", billerId,
                "accountNumber", accountNumber,
                "accountName", "Customer " + accountNumber,
                "amount", 15000L,
                "dueDate", OffsetDateTime.now().plusDays(7).toString()
        );
    }

    @Override
    @Transactional
    public TransactionResponse payBill(UUID userId, BillPaymentRequest request) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId.toString()));

        UUID txnId = UUID.randomUUID();
        walletService.debitWallet(wallet.getId(), request.getAmount(), "Bill payment: " + request.getBillerId(), txnId);

        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .type(TransactionType.BILL_PAY)
                .status(TransactionStatus.COMPLETED)
                .senderWallet(wallet)
                .amount(request.getAmount())
                .fee(0L)
                .currency("MMK")
                .description("Bill payment to " + request.getBillerId())
                .completedAt(OffsetDateTime.now())
                .metadata(Map.of("billerId", request.getBillerId(), "accountNumber", request.getAccountNumber()))
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Bill payment: userId={}, biller={}, amount={}", userId, request.getBillerId(), request.getAmount());

        return TransactionResponse.builder()
                .id(transaction.getId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }

    @Override
    public Map<String, Object> getBillPaymentHistory(UUID userId, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("page", page);
        result.put("size", size);
        return result;
    }
}
