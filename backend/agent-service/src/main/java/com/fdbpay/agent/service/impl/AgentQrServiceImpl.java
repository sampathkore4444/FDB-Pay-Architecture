package com.fdbpay.agent.service.impl;

import com.fdbpay.agent.dto.request.AgentQrGenerateRequest;
import com.fdbpay.agent.dto.response.AgentQrResponse;
import com.fdbpay.agent.model.AgentAccount;
import com.fdbpay.agent.repository.AgentAccountRepository;
import com.fdbpay.agent.service.AgentQrService;
import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentQrServiceImpl implements AgentQrService {

    private static final String QR_CACHE_PREFIX = "agent:qr:";
    private static final long QR_CACHE_TTL_HOURS = 24;

    private final AgentAccountRepository agentAccountRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ApiResponse<AgentQrResponse> generateQr(UUID agentUserId, AgentQrGenerateRequest request) {
        AgentAccount agent = agentAccountRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent account", agentUserId.toString()));

        String qrData;
        if (request.getAmount() != null && request.getAmount() > 0) {
            qrData = "fdbpay://agent/" + agentUserId + "?amount=" + request.getAmount();
        } else {
            qrData = "fdbpay://agent/" + agentUserId;
        }

        AgentQrResponse response = AgentQrResponse.builder()
                .qrData(qrData)
                .agentUserId(agentUserId)
                .agentName("Agent-" + agentUserId.toString().substring(0, 8))
                .build();

        redisTemplate.opsForValue().set(QR_CACHE_PREFIX + agentUserId, response, QR_CACHE_TTL_HOURS, TimeUnit.HOURS);

        log.info("QR generated for agent: userId={}", agentUserId);

        return ApiResponse.success(response);
    }

    @Override
    public ApiResponse<AgentQrResponse> getAgentQr(UUID agentUserId) {
        AgentAccount agent = agentAccountRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent account", agentUserId.toString()));

        Object cached = redisTemplate.opsForValue().get(QR_CACHE_PREFIX + agentUserId);
        if (cached instanceof AgentQrResponse qrResponse) {
            return ApiResponse.success(qrResponse);
        }

        String qrData = "fdbpay://agent/" + agentUserId;
        AgentQrResponse response = AgentQrResponse.builder()
                .qrData(qrData)
                .agentUserId(agentUserId)
                .agentName("Agent-" + agentUserId.toString().substring(0, 8))
                .build();

        redisTemplate.opsForValue().set(QR_CACHE_PREFIX + agentUserId, response, QR_CACHE_TTL_HOURS, TimeUnit.HOURS);

        return ApiResponse.success(response);
    }
}
