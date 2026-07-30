package com.fdbpay.remittance.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.remittance.service.dto.request.InitiateRemittanceRequest;
import com.fdbpay.remittance.service.dto.request.RemittanceWebhookRequest;
import com.fdbpay.remittance.service.dto.response.RemittanceCorridorResponse;
import com.fdbpay.remittance.service.dto.response.RemittanceRateQuoteResponse;
import com.fdbpay.remittance.service.dto.response.RemittanceResponse;
import com.fdbpay.remittance.service.service.RemittanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/remittance")
@RequiredArgsConstructor
public class RemittanceController {

    private final RemittanceService remittanceService;

    @GetMapping("/corridors")
    public ApiResponse<List<RemittanceCorridorResponse>> getCorridors() {
        return ApiResponse.success(remittanceService.getCorridors());
    }

    @GetMapping("/exchange-rates")
    public ApiResponse<List<Map<String, Object>>> getExchangeRates() {
        List<RemittanceCorridorResponse> corridors = remittanceService.getCorridors();
        List<Map<String, Object>> rates = corridors.stream().map(c -> Map.<String, Object>of(
                "from", c.getSourceCurrency(),
                "to", c.getDestCurrency(),
                "rate", c.getExchangeRate(),
                "corridor", c.getCode()
        )).collect(Collectors.toList());
        return ApiResponse.success(rates);
    }

    @GetMapping
    public ApiResponse<Page<RemittanceResponse>> getRemittances(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(remittanceService.getMyRemittances(userId, page, size));
    }

    @PostMapping("/quote")
    public ApiResponse<RemittanceRateQuoteResponse> getRateQuote(
            @RequestParam String corridor,
            @RequestParam Long amount) {
        return ApiResponse.success(remittanceService.getRateQuote(corridor, amount));
    }

    @PostMapping("/initiate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RemittanceResponse> initiateRemittance(
            @RequestParam UUID userId,
            @Valid @RequestBody InitiateRemittanceRequest request) {
        return ApiResponse.success(remittanceService.initiateRemittance(userId, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RemittanceResponse> getRemittance(@PathVariable UUID id) {
        return ApiResponse.success(remittanceService.getRemittance(id));
    }

    @GetMapping("/my")
    public ApiResponse<Page<RemittanceResponse>> getMyRemittances(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(remittanceService.getMyRemittances(userId, page, size));
    }

    @PostMapping("/webhook")
    public ApiResponse<RemittanceResponse> processWebhook(
            @Valid @RequestBody RemittanceWebhookRequest request) {
        return ApiResponse.success(remittanceService.processWebhook(request));
    }
}
