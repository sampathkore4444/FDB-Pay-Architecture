package com.fdbpay.services.impl;

import com.fdbpay.common.constants.ErrorCodes;
import com.fdbpay.common.exceptions.BusinessException;
import com.fdbpay.common.exceptions.InsufficientBalanceException;
import com.fdbpay.common.exceptions.ResourceNotFoundException;
import com.fdbpay.models.entity.AgentAccount;
import com.fdbpay.models.entity.Transaction;
import com.fdbpay.models.entity.User;
import com.fdbpay.models.entity.Wallet;
import com.fdbpay.models.enums.TransactionStatus;
import com.fdbpay.models.enums.TransactionType;
import com.fdbpay.repositories.AgentAccountRepository;
import com.fdbpay.repositories.TransactionRepository;
import com.fdbpay.repositories.UserRepository;
import com.fdbpay.repositories.WalletRepository;
import com.fdbpay.schemas.request.CashInRequest;
import com.fdbpay.schemas.request.CashOutRequest;
import com.fdbpay.schemas.response.AgentAccountResponse;
import com.fdbpay.schemas.response.TransactionResponse;
import com.fdbpay.services.AgentService;
import com.fdbpay.services.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentAccountRepository agentAccountRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;

    @Override
    public AgentAccountResponse getAgentAccount(UUID agentUserId) {
        AgentAccount agent = agentAccountRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent account", agentUserId.toString()));

        return AgentAccountResponse.builder()
                .id(agent.getId())
                .userId(agent.getUser().getId())
                .walletId(agent.getWallet().getId())
                .floatBalance(agent.getFloatBalance())
                .commissionBalance(agent.getCommissionBalance())
                .status(agent.getStatus())
                .dailyLimit(agent.getDailyLimit())
                .createdAt(agent.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public TransactionResponse cashIn(UUID agentUserId, CashInRequest request) {
        AgentAccount agent = agentAccountRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent account", agentUserId.toString()));

        if (agent.getFloatBalance() < request.getAmount()) {
            throw new InsufficientBalanceException(agent.getFloatBalance(), request.getAmount());
        }

        User customer = userRepository.findByPhone(request.getCustomerPhone())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerPhone()));

        Wallet customerWallet = walletRepository.findByUserId(customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer wallet", customer.getId().toString()));

        UUID txnId = UUID.randomUUID();
        walletService.creditWallet(customerWallet.getId(), request.getAmount(), "Cash-in via agent", txnId);

        agent.setFloatBalance(agent.getFloatBalance() - request.getAmount());
        agentAccountRepository.save(agent);

        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .type(TransactionType.CASH_IN)
                .status(TransactionStatus.COMPLETED)
                .receiverWallet(customerWallet)
                .amount(request.getAmount())
                .fee(0L)
                .currency("MMK")
                .description("Cash-in from agent")
                .completedAt(OffsetDateTime.now())
                .metadata(Map.of("agentId", agent.getId().toString(), "customerPhone", request.getCustomerPhone()))
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Cash-in completed: agentId={}, customerPhone={}, amount={}", agentUserId, request.getCustomerPhone(), request.getAmount());

        return TransactionResponse.builder()
                .id(transaction.getId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }

    @Override
    @Transactional
    public TransactionResponse cashOut(UUID agentUserId, CashOutRequest request) {
        AgentAccount agent = agentAccountRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent account", agentUserId.toString()));

        User customer = userRepository.findByPhone(request.getCustomerPhone())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerPhone()));

        Wallet customerWallet = walletRepository.findByUserId(customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer wallet", customer.getId().toString()));

        UUID txnId = UUID.randomUUID();
        walletService.debitWallet(customerWallet.getId(), request.getAmount(), "Cash-out via agent", txnId);

        agent.setFloatBalance(agent.getFloatBalance() + request.getAmount());
        agentAccountRepository.save(agent);

        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .type(TransactionType.CASH_OUT)
                .status(TransactionStatus.COMPLETED)
                .senderWallet(customerWallet)
                .amount(request.getAmount())
                .fee(0L)
                .currency("MMK")
                .description("Cash-out via agent")
                .completedAt(OffsetDateTime.now())
                .metadata(Map.of("agentId", agent.getId().toString(), "customerPhone", request.getCustomerPhone()))
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Cash-out completed: agentId={}, customerPhone={}, amount={}", agentUserId, request.getCustomerPhone(), request.getAmount());

        return TransactionResponse.builder()
                .id(transaction.getId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }

    @Override
    public Map<String, Object> getFloatHistory(UUID agentUserId, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        result.put("agentId", agentUserId);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public Map<String, Object> getCommissionHistory(UUID agentUserId, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        result.put("agentId", agentUserId);
        result.put("page", page);
        result.put("size", size);
        return result;
    }
}
