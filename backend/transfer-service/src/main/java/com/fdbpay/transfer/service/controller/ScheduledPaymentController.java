package com.fdbpay.transfer.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.transfer.service.dto.request.CreateScheduledPaymentRequest;
import com.fdbpay.transfer.service.dto.response.ScheduledPaymentResponse;
import com.fdbpay.transfer.service.service.ScheduledPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transfer/schedule")
@RequiredArgsConstructor
public class ScheduledPaymentController {

    private final ScheduledPaymentService scheduledPaymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ScheduledPaymentResponse> create(
            @RequestParam UUID userId,
            @Valid @RequestBody CreateScheduledPaymentRequest request) {
        return ApiResponse.success(scheduledPaymentService.create(userId, request));
    }

    @GetMapping("/my")
    public ApiResponse<Page<ScheduledPaymentResponse>> getMySchedules(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(scheduledPaymentService.getMySchedules(userId, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<ScheduledPaymentResponse> cancel(
            @PathVariable UUID id,
            @RequestParam UUID userId) {
        return ApiResponse.success(scheduledPaymentService.cancel(id, userId));
    }

    @PutMapping("/{id}/pause")
    public ApiResponse<ScheduledPaymentResponse> pause(
            @PathVariable UUID id,
            @RequestParam UUID userId) {
        return ApiResponse.success(scheduledPaymentService.pause(id, userId));
    }

    @PutMapping("/{id}/resume")
    public ApiResponse<ScheduledPaymentResponse> resume(
            @PathVariable UUID id,
            @RequestParam UUID userId) {
        return ApiResponse.success(scheduledPaymentService.resume(id, userId));
    }
}
