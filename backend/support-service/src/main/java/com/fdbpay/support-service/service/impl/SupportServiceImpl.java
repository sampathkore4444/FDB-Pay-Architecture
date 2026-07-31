package com.fdbpay.support.service.service.impl;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.support.service.dto.request.AddMessageRequest;
import com.fdbpay.support.service.dto.request.AssignManagerRequest;
import com.fdbpay.support.service.dto.request.CreateTicketRequest;
import com.fdbpay.support.service.dto.request.EscalateRequest;
import com.fdbpay.support.service.dto.response.*;
import com.fdbpay.support.service.model.AccountManager;
import com.fdbpay.support.service.model.AccountManager.AccountManagerStatus;
import com.fdbpay.support.service.model.SupportTicket;
import com.fdbpay.support.service.model.TicketMessage;
import com.fdbpay.support.service.model.enums.SenderType;
import com.fdbpay.support.service.model.enums.TicketPriority;
import com.fdbpay.support.service.model.enums.TicketStatus;
import com.fdbpay.support.service.repository.AccountManagerRepository;
import com.fdbpay.support.service.repository.FaqRepository;
import com.fdbpay.support.service.repository.SupportTicketRepository;
import com.fdbpay.support.service.repository.TicketMessageRepository;
import com.fdbpay.support.service.service.SupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportServiceImpl implements SupportService {

    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final AccountManagerRepository managerRepository;
    private final FaqRepository faqRepository;

    @Override
    @Transactional
    public SupportTicketResponse createTicket(UUID userId, CreateTicketRequest request) {
        AccountManager assignedManager = managerRepository.findAvailableManager(AccountManagerStatus.ACTIVE)
                .orElse(null);

        OffsetDateTime slaDeadline = calculateSlaDeadline(request.getPriority());

        SupportTicket ticket = SupportTicket.builder()
                .corporateUserId(userId)
                .subject(request.getSubject())
                .category(request.getCategory())
                .priority(request.getPriority())
                .status(TicketStatus.OPEN)
                .assignedManagerId(assignedManager != null ? assignedManager.getId() : null)
                .slaDeadline(slaDeadline)
                .build();

        ticket = ticketRepository.save(ticket);

        if (assignedManager != null) {
            assignedManager.setCurrentClients(assignedManager.getCurrentClients() + 1);
            managerRepository.save(assignedManager);
        }

        TicketMessage initialMessage = TicketMessage.builder()
                .ticketId(ticket.getId())
                .senderId(userId)
                .senderType(SenderType.CUSTOMER)
                .message(request.getMessage())
                .build();
        messageRepository.save(initialMessage);

        ticket.setLastResponseAt(OffsetDateTime.now());
        ticket = ticketRepository.save(ticket);

        log.info("Ticket created: id={}, userId={}, category={}, priority={}",
                ticket.getId(), userId, request.getCategory(), request.getPriority());
        return mapToResponse(ticket, 1);
    }

    @Override
    public SupportTicketResponse getTicket(UUID ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support Ticket", ticketId.toString()));
        long messageCount = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).size();
        return mapToResponse(ticket, (int) messageCount);
    }

    @Override
    public List<TicketMessageResponse> getTicketMessages(UUID ticketId) {
        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::mapMessageToResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketMessageResponse addMessage(UUID ticketId, UUID userId, AddMessageRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support Ticket", ticketId.toString()));

        SenderType senderType = ticket.getAssignedManagerId() != null
                && ticket.getAssignedManagerId().equals(userId)
                ? SenderType.MANAGER
                : SenderType.CUSTOMER;

        TicketMessage message = TicketMessage.builder()
                .ticketId(ticketId)
                .senderId(userId)
                .senderType(senderType)
                .message(request.getMessage())
                .attachments(request.getAttachments())
                .build();
        message = messageRepository.save(message);

        ticket.setLastResponseAt(OffsetDateTime.now());
        if (ticket.getStatus() == TicketStatus.WAITING_CUSTOMER && senderType == SenderType.CUSTOMER) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        } else if (ticket.getStatus() == TicketStatus.WAITING_INTERNAL && senderType == SenderType.MANAGER) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticketRepository.save(ticket);

        log.info("Message added to ticket: ticketId={}, senderType={}", ticketId, senderType);
        return mapMessageToResponse(message);
    }

    @Override
    public Page<SupportTicketResponse> getAllTickets(int page, int size) {
        Page<SupportTicket> tickets = ticketRepository
                .findAll(PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        return tickets.map(t -> {
            long messageCount = messageRepository.findByTicketIdOrderByCreatedAtAsc(t.getId()).size();
            return mapToResponse(t, (int) messageCount);
        });
    }

    @Override
    public Page<SupportTicketResponse> getMyTickets(UUID userId, int page, int size) {
        Page<SupportTicket> tickets = ticketRepository
                .findByCorporateUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return tickets.map(t -> {
            long messageCount = messageRepository.findByTicketIdOrderByCreatedAtAsc(t.getId()).size();
            return mapToResponse(t, (int) messageCount);
        });
    }

    @Override
    public Page<SupportTicketResponse> getManagerTickets(UUID managerId, int page, int size) {
        Page<SupportTicket> tickets = ticketRepository
                .findByAssignedManagerIdOrderByCreatedAtDesc(managerId, PageRequest.of(page, size));
        return tickets.map(t -> {
            long messageCount = messageRepository.findByTicketIdOrderByCreatedAtAsc(t.getId()).size();
            return mapToResponse(t, (int) messageCount);
        });
    }

    @Override
    @Transactional
    public SupportTicketResponse assignManager(UUID ticketId, AssignManagerRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support Ticket", ticketId.toString()));

        if (ticket.getAssignedManagerId() != null) {
            AccountManager previousManager = managerRepository.findById(ticket.getAssignedManagerId()).orElse(null);
            if (previousManager != null && previousManager.getCurrentClients() > 0) {
                previousManager.setCurrentClients(previousManager.getCurrentClients() - 1);
                managerRepository.save(previousManager);
            }
        }

        UUID managerId = request.getManagerId();
        if (managerId == null) {
            AccountManager availableManager = managerRepository.findAvailableManager(AccountManagerStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.SERVICE_UNAVAILABLE, "No available account managers"));
            managerId = availableManager.getId();
        }
        final UUID resolvedManagerId = managerId;

        AccountManager manager = managerRepository.findById(resolvedManagerId)
                .orElseThrow(() -> new ResourceNotFoundException("Account Manager", resolvedManagerId.toString()));

        ticket.setAssignedManagerId(managerId);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket = ticketRepository.save(ticket);

        manager.setCurrentClients(manager.getCurrentClients() + 1);
        managerRepository.save(manager);

        log.info("Manager assigned to ticket: ticketId={}, managerId={}", ticketId, managerId);
        return mapToResponse(ticket, (int) messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).size());
    }

    @Override
    @Transactional
    public SupportTicketResponse escalate(UUID ticketId, EscalateRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support Ticket", ticketId.toString()));

        if (request.getNewPriority() != null) {
            ticket.setPriority(request.getNewPriority());
            ticket.setSlaDeadline(calculateSlaDeadline(request.getNewPriority()));
        }

        ticket.setStatus(TicketStatus.OPEN);
        ticket = ticketRepository.save(ticket);

        if (request.getReason() != null) {
            TicketMessage systemMessage = TicketMessage.builder()
                    .ticketId(ticketId)
                    .senderId(UUID.randomUUID())
                    .senderType(SenderType.SYSTEM)
                    .message("Ticket escalated. Reason: " + request.getReason())
                    .build();
            messageRepository.save(systemMessage);
        }

        log.info("Ticket escalated: ticketId={}, newPriority={}", ticketId, request.getNewPriority());
        return mapToResponse(ticket, (int) messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).size());
    }

    @Override
    @Transactional
    public SupportTicketResponse resolveTicket(UUID ticketId, UUID userId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support Ticket", ticketId.toString()));

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket = ticketRepository.save(ticket);

        if (ticket.getAssignedManagerId() != null) {
            AccountManager manager = managerRepository.findById(ticket.getAssignedManagerId()).orElse(null);
            if (manager != null && manager.getCurrentClients() > 0) {
                manager.setCurrentClients(manager.getCurrentClients() - 1);
                managerRepository.save(manager);
            }
        }

        TicketMessage systemMessage = TicketMessage.builder()
                .ticketId(ticketId)
                .senderId(userId)
                .senderType(SenderType.MANAGER)
                .message("Ticket resolved.")
                .build();
        messageRepository.save(systemMessage);

        log.info("Ticket resolved: ticketId={}, userId={}", ticketId, userId);
        return mapToResponse(ticket, (int) messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).size());
    }

    @Override
    public TicketStatsResponse getStats() {
        long totalOpen = ticketRepository.countByStatus(TicketStatus.OPEN)
                + ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)
                + ticketRepository.countByStatus(TicketStatus.WAITING_CUSTOMER)
                + ticketRepository.countByStatus(TicketStatus.WAITING_INTERNAL);
        long totalResolved = ticketRepository.countByStatus(TicketStatus.RESOLVED)
                + ticketRepository.countByStatus(TicketStatus.CLOSED);

        Map<String, Long> byStatus = new HashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            long count = ticketRepository.countByStatus(status);
            if (count > 0) {
                byStatus.put(status.name(), count);
            }
        }

        Map<String, Long> byCategory = new HashMap<>();
        List<SupportTicket> allTickets = ticketRepository.findAll();
        for (SupportTicket ticket : allTickets) {
            byCategory.merge(ticket.getCategory().name(), 1L, Long::sum);
        }

        return TicketStatsResponse.builder()
                .totalOpen(totalOpen)
                .totalResolved(totalResolved)
                .avgResponseTimeHours(calculateAvgResponseTime(allTickets))
                .byCategory(byCategory)
                .byStatus(byStatus)
                .build();
    }

    @Override
    public List<AccountManagerResponse> getAvailableManagers() {
        return managerRepository.findByStatus(AccountManagerStatus.ACTIVE)
                .stream()
                .map(this::mapManagerToResponse)
                .toList();
    }

    private OffsetDateTime calculateSlaDeadline(TicketPriority priority) {
        OffsetDateTime now = OffsetDateTime.now();
        return switch (priority) {
            case URGENT -> now.plusHours(4);
            case HIGH -> now.plusHours(8);
            case MEDIUM -> now.plusHours(24);
            case LOW -> now.plusHours(72);
        };
    }

    private double calculateAvgResponseTime(List<SupportTicket> tickets) {
        return tickets.stream()
                .filter(t -> t.getLastResponseAt() != null && t.getCreatedAt() != null)
                .mapToDouble(t -> Duration.between(t.getCreatedAt(), t.getLastResponseAt()).toHours())
                .average()
                .orElse(0.0);
    }

    private SupportTicketResponse mapToResponse(SupportTicket ticket, int messageCount) {
        return SupportTicketResponse.builder()
                .id(ticket.getId())
                .corporateUserId(ticket.getCorporateUserId())
                .subject(ticket.getSubject())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .assignedManagerId(ticket.getAssignedManagerId())
                .messageCount(messageCount)
                .lastResponseAt(ticket.getLastResponseAt())
                .slaDeadline(ticket.getSlaDeadline())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private TicketMessageResponse mapMessageToResponse(TicketMessage message) {
        return TicketMessageResponse.builder()
                .id(message.getId())
                .ticketId(message.getTicketId())
                .senderId(message.getSenderId())
                .senderType(message.getSenderType())
                .message(message.getMessage())
                .attachments(message.getAttachments())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private AccountManagerResponse mapManagerToResponse(AccountManager manager) {
        return AccountManagerResponse.builder()
                .id(manager.getId())
                .userId(manager.getUserId())
                .name(manager.getName())
                .email(manager.getEmail())
                .phone(manager.getPhone())
                .maxClients(manager.getMaxClients())
                .currentClients(manager.getCurrentClients())
                .status(manager.getStatus())
                .createdAt(manager.getCreatedAt())
                .build();
    }

    @Override
    public List<FaqResponse> getAllFaqs() {
        return faqRepository.findAllByOrderBySortOrderAsc().stream()
                .map(faq -> FaqResponse.builder()
                        .id(faq.getId())
                        .question(faq.getQuestion())
                        .answer(faq.getAnswer())
                        .category(faq.getCategory())
                        .sortOrder(faq.getSortOrder())
                        .build())
                .toList();
    }
}
