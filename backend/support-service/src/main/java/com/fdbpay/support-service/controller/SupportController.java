package com.fdbpay.support.service.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.support.service.dto.request.AddMessageRequest;
import com.fdbpay.support.service.dto.request.AssignManagerRequest;
import com.fdbpay.support.service.dto.request.CreateTicketRequest;
import com.fdbpay.support.service.dto.request.EscalateRequest;
import com.fdbpay.support.service.dto.response.*;
import com.fdbpay.support.service.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SupportTicketResponse> createTicket(
            @RequestParam UUID userId,
            @Valid @RequestBody CreateTicketRequest request) {
        return ApiResponse.success(supportService.createTicket(userId, request));
    }

    @GetMapping("/tickets/{id}")
    public ApiResponse<SupportTicketResponse> getTicket(@PathVariable UUID id) {
        return ApiResponse.success(supportService.getTicket(id));
    }

    @PostMapping("/tickets/{id}/messages")
    public ApiResponse<TicketMessageResponse> addMessage(
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @Valid @RequestBody AddMessageRequest request) {
        return ApiResponse.success(supportService.addMessage(id, userId, request));
    }

    @GetMapping("/my-tickets")
    public ApiResponse<Page<SupportTicketResponse>> getMyTickets(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(supportService.getMyTickets(userId, page, size));
    }

    @GetMapping("/manager/{managerId}/tickets")
    public ApiResponse<Page<SupportTicketResponse>> getManagerTickets(
            @PathVariable UUID managerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(supportService.getManagerTickets(managerId, page, size));
    }

    @PutMapping("/tickets/{id}/assign")
    public ApiResponse<SupportTicketResponse> assignManager(
            @PathVariable UUID id,
            @Valid @RequestBody AssignManagerRequest request) {
        return ApiResponse.success(supportService.assignManager(id, request));
    }

    @PutMapping("/tickets/{id}/escalate")
    public ApiResponse<SupportTicketResponse> escalate(
            @PathVariable UUID id,
            @Valid @RequestBody EscalateRequest request) {
        return ApiResponse.success(supportService.escalate(id, request));
    }

    @PutMapping("/tickets/{id}/resolve")
    public ApiResponse<SupportTicketResponse> resolveTicket(
            @PathVariable UUID id,
            @RequestParam UUID userId) {
        return ApiResponse.success(supportService.resolveTicket(id, userId));
    }

    @GetMapping("/stats")
    public ApiResponse<TicketStatsResponse> getStats() {
        return ApiResponse.success(supportService.getStats());
    }

    @GetMapping("/managers")
    public ApiResponse<List<AccountManagerResponse>> getAvailableManagers() {
        return ApiResponse.success(supportService.getAvailableManagers());
    }
}
