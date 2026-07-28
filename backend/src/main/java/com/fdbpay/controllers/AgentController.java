package com.fdbpay.controllers;

import com.fdbpay.schemas.request.CashInRequest;
import com.fdbpay.schemas.request.CashOutRequest;
import com.fdbpay.schemas.response.AgentAccountResponse;
import com.fdbpay.schemas.response.ApiResponse;
import com.fdbpay.schemas.response.TransactionResponse;
import com.fdbpay.services.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping("/account")
    public ResponseEntity<ApiResponse<AgentAccountResponse>> getAgentAccount(@RequestParam UUID agentUserId) {
        AgentAccountResponse response = agentService.getAgentAccount(agentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/cash-in")
    public ResponseEntity<ApiResponse<TransactionResponse>> cashIn(
            @RequestParam UUID agentUserId,
            @Valid @RequestBody CashInRequest request) {
        TransactionResponse response = agentService.cashIn(agentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/cash-out")
    public ResponseEntity<ApiResponse<TransactionResponse>> cashOut(
            @RequestParam UUID agentUserId,
            @Valid @RequestBody CashOutRequest request) {
        TransactionResponse response = agentService.cashOut(agentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/float-history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFloatHistory(
            @RequestParam UUID agentUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(agentService.getFloatHistory(agentUserId, page, size)));
    }

    @GetMapping("/commission-history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCommissionHistory(
            @RequestParam UUID agentUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(agentService.getCommissionHistory(agentUserId, page, size)));
    }
}
