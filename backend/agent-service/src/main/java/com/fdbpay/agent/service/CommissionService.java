package com.fdbpay.agent.service;

import com.fdbpay.agent.dto.request.WithdrawCommissionRequest;
import com.fdbpay.agent.dto.response.CommissionRecordResponse;
import com.fdbpay.agent.dto.response.CommissionSummaryResponse;
import com.fdbpay.shared.dto.ApiResponse;

import java.util.UUID;

public interface CommissionService {

    void recordCommission(UUID agentUserId, UUID transactionId, String type, Long amount);

    ApiResponse<?> getCommissionHistory(UUID agentUserId, int page, int size);

    ApiResponse<CommissionSummaryResponse> getSummary(UUID agentUserId);

    ApiResponse<CommissionRecordResponse> withdrawCommission(UUID agentUserId, WithdrawCommissionRequest request);
}
