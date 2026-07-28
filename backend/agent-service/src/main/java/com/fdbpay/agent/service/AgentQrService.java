package com.fdbpay.agent.service;

import com.fdbpay.agent.dto.request.AgentQrGenerateRequest;
import com.fdbpay.agent.dto.response.AgentQrResponse;
import com.fdbpay.shared.dto.ApiResponse;

import java.util.UUID;

public interface AgentQrService {

    ApiResponse<AgentQrResponse> generateQr(UUID agentUserId, AgentQrGenerateRequest request);

    ApiResponse<AgentQrResponse> getAgentQr(UUID agentUserId);
}
