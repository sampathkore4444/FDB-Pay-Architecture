package com.fdbpay.dispute.service.controller;

import com.fdbpay.dispute.service.dto.request.AddEvidenceRequest;
import com.fdbpay.dispute.service.dto.request.CreateDisputeRequest;
import com.fdbpay.dispute.service.dto.request.ResolveDisputeRequest;
import com.fdbpay.dispute.service.dto.response.DisputeEvidenceResponse;
import com.fdbpay.dispute.service.dto.response.DisputeResponse;
import com.fdbpay.dispute.service.dto.response.DisputeStatsResponse;
import com.fdbpay.dispute.service.model.enums.DisputeStatus;
import com.fdbpay.dispute.service.service.DisputeService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DisputeResponse> createDispute(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateDisputeRequest request) {
        return ApiResponse.success(disputeService.createDispute(userId, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<DisputeResponse> getDispute(@PathVariable UUID id) {
        return ApiResponse.success(disputeService.getDispute(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<DisputeResponse> updateDispute(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDisputeRequest request,
            @RequestHeader("X-User-Id") UUID adminId) {
        return ApiResponse.success(disputeService.updateDispute(id, request, adminId));
    }

    @PutMapping("/{id}/resolve")
    public ApiResponse<DisputeResponse> resolveDispute(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveDisputeRequest request,
            @RequestHeader("X-User-Id") UUID adminId) {
        return ApiResponse.success(disputeService.resolveDispute(id, request, adminId));
    }

    @PostMapping("/{id}/evidence")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DisputeEvidenceResponse> addEvidence(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody AddEvidenceRequest request) {
        return ApiResponse.success(disputeService.addEvidence(id, userId, request));
    }

    @GetMapping("/{id}/evidence")
    public ApiResponse<List<DisputeEvidenceResponse>> getDisputeEvidence(@PathVariable UUID id) {
        return ApiResponse.success(disputeService.getDisputeEvidence(id));
    }

    @GetMapping("/my")
    public ApiResponse<Page<DisputeResponse>> getMyDisputes(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(disputeService.getMyDisputes(userId, page, size));
    }

    @GetMapping("/all")
    public ApiResponse<Page<DisputeResponse>> getAllDisputes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) DisputeStatus status) {
        return ApiResponse.success(disputeService.getAllDisputes(page, size, status));
    }

    @GetMapping("/stats")
    public ApiResponse<DisputeStatsResponse> getStats() {
        return ApiResponse.success(disputeService.getStats());
    }
}
