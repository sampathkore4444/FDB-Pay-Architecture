package com.fdbpay.kyc.service.controller;

import com.fdbpay.kyc.service.dto.request.AdminKycReviewRequest;
import com.fdbpay.kyc.service.dto.response.AdminKycRequestResponse;
import com.fdbpay.kyc.service.dto.response.KycStatusResponse;
import com.fdbpay.kyc.service.service.KycService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/kyc")
@RequiredArgsConstructor
public class AdminKycController {

    private final KycService kycService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<AdminKycRequestResponse> requests = kycService.getAdminRequests(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(Map.of("requests", requests)));
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<ApiResponse<KycStatusResponse>> reviewRequest(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) UUID reviewedBy,
            @Valid @RequestBody AdminKycReviewRequest request) {
        KycStatusResponse response = kycService.reviewRequest(id, request.getStatus(), request.getReason(), reviewedBy);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
