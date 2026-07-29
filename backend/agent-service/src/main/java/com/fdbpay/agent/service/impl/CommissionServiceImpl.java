package com.fdbpay.agent.service.impl;

import com.fdbpay.agent.dto.request.WithdrawCommissionRequest;
import com.fdbpay.agent.dto.response.CommissionRecordResponse;
import com.fdbpay.agent.dto.response.CommissionSummaryResponse;
import com.fdbpay.agent.model.AgentAccount;
import com.fdbpay.agent.model.CommissionRecord;
import com.fdbpay.agent.repository.AgentAccountRepository;
import com.fdbpay.agent.repository.CommissionRecordRepository;
import com.fdbpay.agent.service.CommissionService;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.InsufficientBalanceException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private static final BigDecimal CASH_IN_RATE = new BigDecimal("0.005");
    private static final BigDecimal CASH_OUT_RATE = new BigDecimal("0.003");

    private final CommissionRecordRepository commissionRecordRepository;
    private final AgentAccountRepository agentAccountRepository;
    private final WebClient walletWebClient;

    @Override
    @Transactional
    public void recordCommission(UUID agentUserId, UUID transactionId, String type, Long amount) {
        BigDecimal commissionRate = "CASH_IN".equals(type) ? CASH_IN_RATE : CASH_OUT_RATE;
        Long commissionAmount = BigDecimal.valueOf(amount).multiply(commissionRate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        if (commissionAmount <= 0) {
            return;
        }

        AgentAccount agent = agentAccountRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent account", agentUserId.toString()));

        CommissionRecord record = CommissionRecord.builder()
                .id(UUID.randomUUID())
                .agentUserId(agentUserId)
                .transactionId(transactionId)
                .type(type)
                .amount(amount)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .status("EARNED")
                .earnedAt(OffsetDateTime.now())
                .build();
        commissionRecordRepository.save(record);

        agent.setCommissionBalance(agent.getCommissionBalance() + commissionAmount);
        agentAccountRepository.save(agent);

        log.info("Commission recorded: agent={}, txnId={}, type={}, amount={}, commission={}",
                agentUserId, transactionId, type, amount, commissionAmount);
    }

    @Override
    public ApiResponse<?> getCommissionHistory(UUID agentUserId, int page, int size) {
        Page<CommissionRecord> records = commissionRecordRepository
                .findByAgentUserIdOrderByCreatedAtDesc(agentUserId, PageRequest.of(page, size));

        var response = records.map(this::mapToResponse);

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

    @Override
    public ApiResponse<CommissionSummaryResponse> getSummary(UUID agentUserId) {
        Long totalEarned = commissionRecordRepository
                .sumCommissionAmountByAgentUserIdAndStatus(agentUserId, "EARNED");
        Long totalPaid = commissionRecordRepository
                .sumCommissionAmountByAgentUserIdAndStatus(agentUserId, "PAID");
        Long withdrawn = commissionRecordRepository
                .sumCommissionAmountByAgentUserIdAndStatus(agentUserId, "WITHDRAWN");

        AgentAccount agent = agentAccountRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent account", agentUserId.toString()));

        List<CommissionRecord> withdrawalRecords = commissionRecordRepository
                .findByAgentUserIdAndStatusOrderByCreatedAtDesc(agentUserId, "WITHDRAWN");
        List<CommissionRecordResponse> withdrawalHistory = withdrawalRecords.stream()
                .map(this::mapToResponse)
                .toList();

        CommissionSummaryResponse summary = CommissionSummaryResponse.builder()
                .totalEarned(totalEarned)
                .totalPaid(totalPaid)
                .pendingBalance(agent.getCommissionBalance())
                .withdrawalHistory(withdrawalHistory)
                .build();

        return ApiResponse.success(summary);
    }

    @Override
    @Transactional
    public ApiResponse<CommissionRecordResponse> withdrawCommission(UUID agentUserId, WithdrawCommissionRequest request) {
        AgentAccount agent = agentAccountRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent account", agentUserId.toString()));

        if (agent.getCommissionBalance() < request.getAmount()) {
            throw new InsufficientBalanceException(agent.getCommissionBalance(), request.getAmount());
        }

        agent.setCommissionBalance(agent.getCommissionBalance() - request.getAmount());
        agentAccountRepository.save(agent);

        walletWebClient.post()
                .uri(uriBuilder -> uriBuilder.path("/wallet/credit")
                        .queryParam("userId", agentUserId)
                        .build())
                .bodyValue(Map.of(
                        "amount", request.getAmount(),
                        "idempotencyKey", "COMM_WD_" + UUID.randomUUID()
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        CommissionRecord record = CommissionRecord.builder()
                .id(UUID.randomUUID())
                .agentUserId(agentUserId)
                .transactionId(UUID.randomUUID())
                .type("WITHDRAWAL")
                .amount(request.getAmount())
                .commissionRate(BigDecimal.ZERO)
                .commissionAmount(request.getAmount())
                .status("WITHDRAWN")
                .earnedAt(OffsetDateTime.now())
                .paidAt(OffsetDateTime.now())
                .build();
        commissionRecordRepository.save(record);

        log.info("Commission withdrawn: agent={}, amount={}, newBalance={}",
                agentUserId, request.getAmount(), agent.getCommissionBalance());

        return ApiResponse.success(mapToResponse(record));
    }

    private CommissionRecordResponse mapToResponse(CommissionRecord record) {
        return CommissionRecordResponse.builder()
                .id(record.getId())
                .agentUserId(record.getAgentUserId())
                .transactionId(record.getTransactionId())
                .type(record.getType())
                .amount(record.getAmount())
                .commissionRate(record.getCommissionRate())
                .commissionAmount(record.getCommissionAmount())
                .status(record.getStatus())
                .earnedAt(record.getEarnedAt())
                .paidAt(record.getPaidAt())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
