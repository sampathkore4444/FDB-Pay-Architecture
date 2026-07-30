package com.fdbpay.support.service.service;

import com.fdbpay.support.service.dto.request.AddMessageRequest;
import com.fdbpay.support.service.dto.request.AssignManagerRequest;
import com.fdbpay.support.service.dto.request.CreateTicketRequest;
import com.fdbpay.support.service.dto.request.EscalateRequest;
import com.fdbpay.support.service.dto.response.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface SupportService {

    SupportTicketResponse createTicket(UUID userId, CreateTicketRequest request);

    SupportTicketResponse getTicket(UUID ticketId);

    TicketMessageResponse addMessage(UUID ticketId, UUID userId, AddMessageRequest request);

    Page<SupportTicketResponse> getAllTickets(int page, int size);

    Page<SupportTicketResponse> getMyTickets(UUID userId, int page, int size);

    Page<SupportTicketResponse> getManagerTickets(UUID managerId, int page, int size);

    SupportTicketResponse assignManager(UUID ticketId, AssignManagerRequest request);

    SupportTicketResponse escalate(UUID ticketId, EscalateRequest request);

    SupportTicketResponse resolveTicket(UUID ticketId, UUID userId);

    TicketStatsResponse getStats();

    List<AccountManagerResponse> getAvailableManagers();

    List<FaqResponse> getAllFaqs();
}
