package com.fdbpay.agent.controller;

import com.fdbpay.agent.dto.request.AgentQrGenerateRequest;
import com.fdbpay.agent.service.AgentQrService;
import com.fdbpay.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/agent/qr")
@RequiredArgsConstructor
public class AgentQrController {

    private final AgentQrService agentQrService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<?>> generateQr(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody(required = false) AgentQrGenerateRequest request) {
        if (request == null) {
            request = new AgentQrGenerateRequest();
        }
        var response = agentQrService.generateQr(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<?>> getCurrentQr(
            @RequestHeader("X-User-Id") UUID userId) {
        var response = agentQrService.getAgentQr(userId);
        return ResponseEntity.ok(response);
    }
}
