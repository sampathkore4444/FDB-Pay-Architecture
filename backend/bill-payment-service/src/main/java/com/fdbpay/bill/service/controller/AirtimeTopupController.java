package com.fdbpay.bill.service.controller;

import com.fdbpay.bill.service.dto.request.AirtimeTopupRequest;
import com.fdbpay.bill.service.dto.response.AirtimeTopupResponse;
import com.fdbpay.bill.service.model.enums.AirtimeProvider;
import com.fdbpay.bill.service.service.AirtimeTopupService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/airtime")
@RequiredArgsConstructor
public class AirtimeTopupController {

    private final AirtimeTopupService airtimeTopupService;

    @PostMapping("/topup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AirtimeTopupResponse> topup(
            @RequestParam UUID userId,
            @Valid @RequestBody AirtimeTopupRequest request) {
        return ApiResponse.success(airtimeTopupService.topup(userId, request));
    }

    @GetMapping("/history")
    public ApiResponse<Page<AirtimeTopupResponse>> getHistory(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(airtimeTopupService.getHistory(userId, page, size));
    }

    @GetMapping("/providers")
    public ApiResponse<List<AirtimeProvider>> getProviders() {
        return ApiResponse.success(airtimeTopupService.getProviders());
    }
}
