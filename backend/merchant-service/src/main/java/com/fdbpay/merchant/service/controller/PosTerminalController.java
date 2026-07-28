package com.fdbpay.merchant.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.merchant.service.dto.request.CreatePosTerminalRequest;
import com.fdbpay.merchant.service.dto.response.PosTerminalResponse;
import com.fdbpay.merchant.service.model.enums.PosTerminalStatus;
import com.fdbpay.merchant.service.service.PosTerminalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/{merchantId}/pos")
@RequiredArgsConstructor
public class PosTerminalController {

    private final PosTerminalService posTerminalService;

    @PostMapping("/register")
    public ApiResponse<PosTerminalResponse> register(
            @PathVariable UUID merchantId,
            @Valid @RequestBody CreatePosTerminalRequest request) {
        return ApiResponse.success(posTerminalService.register(merchantId, request));
    }

    @GetMapping
    public ApiResponse<List<PosTerminalResponse>> getTerminals(@PathVariable UUID merchantId) {
        return ApiResponse.success(posTerminalService.getTerminals(merchantId));
    }

    @PutMapping("/{terminalId}/status")
    public ApiResponse<PosTerminalResponse> updateStatus(
            @PathVariable UUID merchantId,
            @PathVariable UUID terminalId,
            @RequestParam PosTerminalStatus status) {
        return ApiResponse.success(posTerminalService.updateStatus(terminalId, status));
    }

    @PostMapping("/{terminalId}/heartbeat")
    public ApiResponse<PosTerminalResponse> heartbeat(
            @PathVariable UUID merchantId,
            @PathVariable UUID terminalId) {
        return ApiResponse.success(posTerminalService.heartbeat(terminalId));
    }
}
