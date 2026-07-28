package com.fdbpay.corporate.controller;

import com.fdbpay.corporate.dto.request.ApproveRequest;
import com.fdbpay.corporate.dto.request.RejectRequest;
import com.fdbpay.corporate.dto.request.SubmitForApprovalRequest;
import com.fdbpay.corporate.service.ApprovalService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/corp/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<?>> submitForApproval(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody SubmitForApprovalRequest request) {
        var response = approvalService.submitForApproval(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<?>> approve(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID approverId,
            @RequestBody(required = false) ApproveRequest request) {
        if (request == null) {
            request = new ApproveRequest();
        }
        var response = approvalService.approve(id, approverId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<?>> reject(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID approverId,
            @Valid @RequestBody RejectRequest request) {
        var response = approvalService.reject(id, approverId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<?>> getPendingApprovals(
            @RequestHeader("X-User-Id") UUID approverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var response = approvalService.getPendingApprovals(approverId, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{disbursementId}/history")
    public ResponseEntity<ApiResponse<?>> getApprovalHistory(
            @PathVariable UUID disbursementId) {
        var response = approvalService.getApprovalHistory(disbursementId);
        return ResponseEntity.ok(response);
    }
}
