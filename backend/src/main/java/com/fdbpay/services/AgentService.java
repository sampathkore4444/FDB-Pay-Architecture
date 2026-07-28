package com.fdbpay.services;

import com.fdbpay.schemas.request.CashInRequest;
import com.fdbpay.schemas.request.CashOutRequest;
import com.fdbpay.schemas.response.AgentAccountResponse;
import com.fdbpay.schemas.response.TransactionResponse;

import java.util.Map;
import java.util.UUID;

public interface AgentService {

    AgentAccountResponse getAgentAccount(UUID agentUserId);

    TransactionResponse cashIn(UUID agentUserId, CashInRequest request);

    TransactionResponse cashOut(UUID agentUserId, CashOutRequest request);

    Map<String, Object> getFloatHistory(UUID agentUserId, int page, int size);

    Map<String, Object> getCommissionHistory(UUID agentUserId, int page, int size);
}
