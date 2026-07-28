package com.fdbpay.transfer.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.transfer.service.dto.request.CreateMoneyRequestRequest;
import com.fdbpay.transfer.service.dto.request.RespondMoneyRequestRequest;
import com.fdbpay.transfer.service.dto.response.MoneyRequestResponse;
import com.fdbpay.transfer.service.service.MoneyRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transfer/request")
@RequiredArgsConstructor
public class MoneyRequestController {

    private final MoneyRequestService moneyRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MoneyRequestResponse> create(
            @RequestParam UUID userId,
            @Valid @RequestBody CreateMoneyRequestRequest request) {
        return ApiResponse.success(moneyRequestService.create(userId, request));
    }

    @PutMapping("/{id}/respond")
    public ApiResponse<MoneyRequestResponse> respond(
            @PathVariable UUID id,
            @RequestParam UUID targetUserId,
            @Valid @RequestBody RespondMoneyRequestRequest request) {
        return ApiResponse.success(moneyRequestService.respond(id, request, targetUserId));
    }

    @GetMapping("/my")
    public ApiResponse<Page<MoneyRequestResponse>> getMyRequests(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(moneyRequestService.getMyRequests(userId, page, size));
    }

    @GetMapping("/phone/{phone}")
    public ApiResponse<Page<MoneyRequestResponse>> getByPhone(
            @PathVariable String phone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(moneyRequestService.getByPhone(phone, page, size));
    }
}
