package com.fdbpay.merchant.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.merchant.service.dto.request.AddStaffRequest;
import com.fdbpay.merchant.service.dto.response.StaffAccountResponse;
import com.fdbpay.merchant.service.model.enums.StaffRole;
import com.fdbpay.merchant.service.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    public ApiResponse<StaffAccountResponse> addStaff(
            @RequestParam UUID merchantId,
            @RequestParam UUID userId,
            @Valid @RequestBody AddStaffRequest request) {
        return ApiResponse.success(staffService.addStaff(merchantId, userId, request));
    }

    @GetMapping
    public ApiResponse<List<StaffAccountResponse>> getStaff(@RequestParam UUID merchantId) {
        return ApiResponse.success(staffService.getStaffByMerchant(merchantId));
    }

    @DeleteMapping("/{staffId}")
    public ApiResponse<Void> removeStaff(
            @RequestParam UUID merchantId,
            @PathVariable UUID staffId) {
        staffService.removeStaff(staffId, merchantId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{staffId}/role")
    public ApiResponse<StaffAccountResponse> updateStaffRole(
            @RequestParam UUID merchantId,
            @PathVariable UUID staffId,
            @RequestParam StaffRole role) {
        return ApiResponse.success(staffService.updateStaffRole(staffId, role, merchantId));
    }
}
