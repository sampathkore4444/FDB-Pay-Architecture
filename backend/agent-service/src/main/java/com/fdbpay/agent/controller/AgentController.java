package com.fdbpay.agent.controller;

import com.fdbpay.agent.dto.request.CashInRequest;
import com.fdbpay.agent.dto.request.CashOutRequest;
import com.fdbpay.agent.service.AgentService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping("/account")
    public ResponseEntity<ApiResponse<?>> getAccount(@RequestHeader("X-User-Id") UUID userId) {
        var account = agentService.getAgentAccount(userId);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping("/cash-in")
    public ResponseEntity<ApiResponse<?>> cashIn(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CashInRequest request) {
        var response = agentService.cashIn(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cash-out")
    public ResponseEntity<ApiResponse<?>> cashOut(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CashOutRequest request) {
        var response = agentService.cashOut(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/float-history")
    public ResponseEntity<ApiResponse<?>> getFloatHistory(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var response = agentService.getFloatHistory(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }
}
