package com.fdbpay.kyc.service.controller;

import com.fdbpay.kyc.service.dto.request.KycReviewRequest;
import com.fdbpay.kyc.service.dto.request.KycSubmitRequest;
import com.fdbpay.kyc.service.dto.response.KycStatusResponse;
import com.fdbpay.kyc.service.service.KycService;
import com.fdbpay.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC", description = "Know Your Customer verification endpoints")
public class KycController {

    private final KycService kycService;

    @PostMapping("/submit")
    @Operation(summary = "Submit KYC documents for verification")
    public ResponseEntity<ApiResponse<KycStatusResponse>> submitDocuments(
            @RequestParam UUID userId,
            @Valid @RequestBody KycSubmitRequest request) {
        KycStatusResponse response = kycService.submitDocuments(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{userId}/review")
    @Operation(summary = "Review and approve/reject KYC submission")
    public ResponseEntity<ApiResponse<KycStatusResponse>> reviewKyc(
            @PathVariable UUID userId,
            @Valid @RequestBody KycReviewRequest request,
            @RequestParam String reviewedBy) {
        KycStatusResponse response = kycService.reviewKyc(userId, request, reviewedBy);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{userId}/status")
    @Operation(summary = "Get KYC status for a user")
    public ResponseEntity<ApiResponse<KycStatusResponse>> getKycStatus(@PathVariable UUID userId) {
        KycStatusResponse response = kycService.getKycStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending KYC requests")
    public ResponseEntity<ApiResponse<Page<KycStatusResponse>>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<KycStatusResponse> pending = kycService.getPendingRequests(page, size);
        return ResponseEntity.ok(ApiResponse.success(pending));
    }
}
