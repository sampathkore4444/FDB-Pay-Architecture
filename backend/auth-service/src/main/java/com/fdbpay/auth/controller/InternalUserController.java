package com.fdbpay.auth.controller;

import com.fdbpay.auth.dto.request.UpdateUserRoleRequest;
import com.fdbpay.auth.service.AuthService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final AuthService authService;

    @PutMapping("/{id}/role")
    public ApiResponse<Void> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        authService.updateUserRole(id, request.getRole());
        return ApiResponse.success(null);
    }
}
