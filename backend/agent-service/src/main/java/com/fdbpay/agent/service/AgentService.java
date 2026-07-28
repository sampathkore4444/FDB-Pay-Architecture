package com.fdbpay.agent.service;

import com.fdbpay.agent.dto.request.CashInRequest;
import com.fdbpay.agent.dto.request.CashOutRequest;
import com.fdbpay.agent.dto.response.AgentAccountResponse;
import com.fdbpay.agent.dto.response.AgentTransactionResponse;
import com.fdbpay.shared.dto.ApiResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AgentService {

    AgentAccountResponse getAgentAccount(UUID userId);

    ApiResponse<AgentTransactionResponse> cashIn(UUID agentUserId, CashInRequest request);

    ApiResponse<AgentTransactionResponse> cashOut(UUID agentUserId, CashOutRequest request);

    ApiResponse<?> getFloatHistory(UUID userId, Pageable pageable);
}
