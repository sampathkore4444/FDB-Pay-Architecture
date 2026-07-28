package com.fdbpay.agent.service.impl;

import com.fdbpay.agent.dto.request.CashInRequest;
import com.fdbpay.agent.dto.request.CashOutRequest;
import com.fdbpay.agent.dto.response.AgentAccountResponse;
import com.fdbpay.agent.dto.response.AgentTransactionResponse;
import com.fdbpay.agent.model.AgentAccount;
import com.fdbpay.agent.model.AgentTransaction;
import com.fdbpay.agent.repository.AgentAccountRepository;
import com.fdbpay.agent.repository.AgentTransactionRepository;
import com.fdbpay.agent.service.AgentService;
import com.fdbpay.shared.constants.AppConstants;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.shared.event.TransactionEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.InsufficientBalanceException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentAccountRepository agentAccountRepository;
    private final AgentTransactionRepository agentTransactionRepository;
    private final WebClient walletWebClient;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Override
    public AgentAccountResponse getAgentAccount(UUID userId) {
        AgentAccount agent = agentAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent account", userId.toString()));
        return mapToAccountResponse(agent);
    }

    @Override
    @Transactional
    public ApiResponse<AgentTransactionResponse> cashIn(UUID agentUserId, CashInRequest request) {
        AgentAccount agent = agentAccountRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent account", agentUserId.toString()));

        if (!"ACTIVE".equals(agent.getStatus())) {
            throw new BusinessException(ErrorCodes.ACCOUNT_SUSPENDED, "Agent account is not active");
        }

        if (agent.getFloatBalance() < request.getAmount()) {
            throw new InsufficientBalanceException(agent.getFloatBalance(), request.getAmount());
        }

        agent.setFloatBalance(agent.getFloatBalance() - request.getAmount());
        agentAccountRepository.save(agent);

        walletWebClient.post()
                .uri("/api/wallets/credit")
                .bodyValue(Map.of(
                        "phone", request.getCustomerPhone(),
                        "amount", request.getAmount(),
                        "reference", "CASH_IN_" + UUID.randomUUID()
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        AgentTransaction transaction = AgentTransaction.builder()
                .id(UUID.randomUUID())
                .agentUserId(agentUserId)
                .customerPhone(request.getCustomerPhone())
                .type("CASH_IN")
                .amount(request.getAmount())
                .status("COMPLETED")
                .build();
        agentTransactionRepository.save(transaction);

        TransactionEvent event = TransactionEvent.builder()
                .transactionId(transaction.getId())
                .type("CASH_IN")
                .status("COMPLETED")
                .senderUserId(agentUserId)
                .amount(request.getAmount())
                .currency("KES")
                .timestamp(OffsetDateTime.now())
                .metadata(Map.of("customerPhone", request.getCustomerPhone(), "idempotencyKey", request.getIdempotencyKey()))
                .build();
        kafkaTemplate.send(AppConstants.TOPIC_TXN_COMPLETED, event);

        log.info("Cash-in completed: agent={}, customer={}, amount={}", agentUserId, request.getCustomerPhone(), request.getAmount());

        return ApiResponse.success(mapToTransactionResponse(transaction));
    }

    @Override
    @Transactional
    public ApiResponse<AgentTransactionResponse> cashOut(UUID agentUserId, CashOutRequest request) {
        AgentAccount agent = agentAccountRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent account", agentUserId.toString()));

        if (!"ACTIVE".equals(agent.getStatus())) {
            throw new BusinessException(ErrorCodes.ACCOUNT_SUSPENDED, "Agent account is not active");
        }

        walletWebClient.post()
                .uri("/api/wallets/debit")
                .bodyValue(Map.of(
                        "phone", request.getCustomerPhone(),
                        "amount", request.getAmount(),
                        "reference", "CASH_OUT_" + UUID.randomUUID()
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        agent.setFloatBalance(agent.getFloatBalance() + request.getAmount());
        agentAccountRepository.save(agent);

        AgentTransaction transaction = AgentTransaction.builder()
                .id(UUID.randomUUID())
                .agentUserId(agentUserId)
                .customerPhone(request.getCustomerPhone())
                .type("CASH_OUT")
                .amount(request.getAmount())
                .status("COMPLETED")
                .build();
        agentTransactionRepository.save(transaction);

        TransactionEvent event = TransactionEvent.builder()
                .transactionId(transaction.getId())
                .type("CASH_OUT")
                .status("COMPLETED")
                .receiverUserId(agentUserId)
                .amount(request.getAmount())
                .currency("KES")
                .timestamp(OffsetDateTime.now())
                .metadata(Map.of("customerPhone", request.getCustomerPhone(), "idempotencyKey", request.getIdempotencyKey()))
                .build();
        kafkaTemplate.send(AppConstants.TOPIC_TXN_COMPLETED, event);

        log.info("Cash-out completed: agent={}, customer={}, amount={}", agentUserId, request.getCustomerPhone(), request.getAmount());

        return ApiResponse.success(mapToTransactionResponse(transaction));
    }

    @Override
    public ApiResponse<?> getFloatHistory(UUID userId, Pageable pageable) {
        agentAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent account", userId.toString()));

        Page<AgentTransaction> transactions = agentTransactionRepository.findByAgentUserIdOrderByCreatedAtDesc(userId, pageable);

        var response = transactions.map(this::mapToTransactionResponse);

        ApiResponse.Pagination pagination = ApiResponse.Pagination.builder()
                .page(response.getNumber())
                .perPage(response.getSize())
                .total(response.getTotalElements())
                .totalPages(response.getTotalPages())
                .build();

        ApiResponse.Meta meta = ApiResponse.Meta.builder()
                .requestId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now().toString())
                .pagination(pagination)
                .build();

        return ApiResponse.<Object>builder()
                .success(true)
                .data(response.getContent())
                .meta(meta)
                .build();
    }

    private AgentAccountResponse mapToAccountResponse(AgentAccount agent) {
        return AgentAccountResponse.builder()
                .id(agent.getId())
                .userId(agent.getUserId())
                .walletId(agent.getWalletId())
                .floatBalance(agent.getFloatBalance())
                .commissionBalance(agent.getCommissionBalance())
                .status(agent.getStatus())
                .dailyLimit(agent.getDailyLimit())
                .createdAt(agent.getCreatedAt())
                .build();
    }

    private AgentTransactionResponse mapToTransactionResponse(AgentTransaction transaction) {
        return AgentTransactionResponse.builder()
                .id(transaction.getId())
                .agentUserId(transaction.getAgentUserId())
                .customerPhone(transaction.getCustomerPhone())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
