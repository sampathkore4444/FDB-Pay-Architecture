package com.fdbpay.auth.controller;

import com.fdbpay.auth.dto.request.UpdateUserStatusRequest;
import com.fdbpay.auth.dto.response.AdminUserResponse;
import com.fdbpay.auth.model.enums.UserStatus;
import com.fdbpay.auth.service.AuthService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<AdminUserResponse> users = authService.getUsers(search, status, page, size);
        return ApiResponse.success(Map.of("users", users));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        authService.updateUserStatus(id, request.getStatus(), request.getReason());
        return ApiResponse.success(null);
    }
}
