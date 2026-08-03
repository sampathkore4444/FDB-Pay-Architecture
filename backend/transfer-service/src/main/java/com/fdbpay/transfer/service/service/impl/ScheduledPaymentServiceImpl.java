package com.fdbpay.transfer.service.service.impl;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.transfer.service.dto.request.CreateScheduledPaymentRequest;
import com.fdbpay.transfer.service.dto.response.ScheduledPaymentResponse;
import com.fdbpay.transfer.service.dto.request.TransferRequest;
import com.fdbpay.transfer.service.model.ScheduledPayment;
import com.fdbpay.transfer.service.model.enums.PaymentFrequency;
import com.fdbpay.transfer.service.model.enums.ScheduledPaymentStatus;
import com.fdbpay.transfer.service.model.enums.TransactionType;
import com.fdbpay.transfer.service.repository.ScheduledPaymentRepository;
import com.fdbpay.transfer.service.service.ScheduledPaymentService;
import com.fdbpay.transfer.service.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledPaymentServiceImpl implements ScheduledPaymentService {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final TransferService transferService;

    private static final int DEFAULT_MAX_EXECUTIONS = 12;

    @Override
    @Transactional
    public ScheduledPaymentResponse create(UUID userId, CreateScheduledPaymentRequest request) {
        LocalDate nextExecutionDate = calculateNextExecutionDate(request.getFrequency(), LocalDate.now());

        ScheduledPayment schedule = ScheduledPayment.builder()
                .userId(userId)
                .recipientIdentifier(request.getRecipientIdentifier())
                .amount(request.getAmount())
                .type(request.getType())
                .frequency(request.getFrequency())
                .nextExecutionDate(nextExecutionDate)
                .status(ScheduledPaymentStatus.ACTIVE)
                .description(request.getDescription())
                .totalExecutions(DEFAULT_MAX_EXECUTIONS)
                .completedExecutions(0)
                .build();

        schedule = scheduledPaymentRepository.save(schedule);
        log.info("Scheduled payment created: id={}, userId={}, frequency={}, amount={}",
                schedule.getId(), userId, request.getFrequency(), request.getAmount());

        return mapToResponse(schedule);
    }

    @Override
    public Page<ScheduledPaymentResponse> getMySchedules(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ScheduledPayment> schedules = scheduledPaymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);
        return schedules.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ScheduledPaymentResponse cancel(UUID scheduleId, UUID userId) {
        ScheduledPayment schedule = scheduledPaymentRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledPayment", scheduleId.toString()));

        if (!schedule.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Not authorized to cancel this schedule");
        }

        if (schedule.getStatus() != ScheduledPaymentStatus.ACTIVE && schedule.getStatus() != ScheduledPaymentStatus.PAUSED) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Cannot cancel schedule in status: " + schedule.getStatus());
        }

        schedule.setStatus(ScheduledPaymentStatus.CANCELLED);
        schedule = scheduledPaymentRepository.save(schedule);
        log.info("Scheduled payment cancelled: id={}", scheduleId);

        return mapToResponse(schedule);
    }

    @Override
    @Transactional
    public ScheduledPaymentResponse pause(UUID scheduleId, UUID userId) {
        ScheduledPayment schedule = scheduledPaymentRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledPayment", scheduleId.toString()));

        if (!schedule.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Not authorized to pause this schedule");
        }

        if (schedule.getStatus() != ScheduledPaymentStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Cannot pause schedule in status: " + schedule.getStatus());
        }

        schedule.setStatus(ScheduledPaymentStatus.PAUSED);
        schedule = scheduledPaymentRepository.save(schedule);
        log.info("Scheduled payment paused: id={}", scheduleId);

        return mapToResponse(schedule);
    }

    @Override
    @Transactional
    public ScheduledPaymentResponse resume(UUID scheduleId, UUID userId) {
        ScheduledPayment schedule = scheduledPaymentRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledPayment", scheduleId.toString()));

        if (!schedule.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Not authorized to resume this schedule");
        }

        if (schedule.getStatus() != ScheduledPaymentStatus.PAUSED) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Cannot resume schedule in status: " + schedule.getStatus());
        }

        schedule.setStatus(ScheduledPaymentStatus.ACTIVE);
        schedule.setNextExecutionDate(LocalDate.now());
        schedule = scheduledPaymentRepository.save(schedule);
        log.info("Scheduled payment resumed: id={}", scheduleId);

        return mapToResponse(schedule);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 1 * * ?")
    public void executeDueSchedules() {
        List<ScheduledPayment> dueSchedules = scheduledPaymentRepository
                .findByStatusAndNextExecutionDateBefore(ScheduledPaymentStatus.ACTIVE, LocalDate.now().plusDays(1));

        log.info("Executing {} due scheduled payments", dueSchedules.size());

        for (ScheduledPayment schedule : dueSchedules) {
            try {
                executeSchedule(schedule);
            } catch (Exception e) {
                log.error("Failed to execute scheduled payment: id={}", schedule.getId(), e);
            }
        }
    }

    private void executeSchedule(ScheduledPayment schedule) {
        String idempotencyKey = "sched_" + schedule.getId() + "_" + schedule.getCompletedExecutions() + "_" + LocalDate.now();

        TransferRequest transferRequest = TransferRequest.builder()
                .idempotencyKey(idempotencyKey)
                .type(TransactionType.P2P)
                .recipientIdentifier(schedule.getRecipientIdentifier())
                .amount(schedule.getAmount())
                .description(schedule.getDescription() != null ? schedule.getDescription() : "Scheduled payment")
                .build();

        transferService.initiateTransfer(schedule.getUserId(), transferRequest);

        schedule.setCompletedExecutions(schedule.getCompletedExecutions() + 1);
        schedule.setLastExecutionDate(LocalDate.now());

        if (schedule.getCompletedExecutions() >= schedule.getTotalExecutions()) {
            schedule.setStatus(ScheduledPaymentStatus.COMPLETED);
            schedule.setNextExecutionDate(null);
            log.info("Scheduled payment completed (max executions reached): id={}", schedule.getId());
        } else {
            schedule.setNextExecutionDate(calculateNextExecutionDate(schedule.getFrequency(), LocalDate.now()));
        }

        scheduledPaymentRepository.save(schedule);
        log.info("Scheduled payment executed: id={}, execution={}/{}",
                schedule.getId(), schedule.getCompletedExecutions(), schedule.getTotalExecutions());
    }

    private LocalDate calculateNextExecutionDate(PaymentFrequency frequency, LocalDate fromDate) {
        return switch (frequency) {
            case DAILY -> fromDate.plusDays(1);
            case WEEKLY -> fromDate.plusWeeks(1);
            case MONTHLY -> fromDate.plusMonths(1);
        };
    }

    private ScheduledPaymentResponse mapToResponse(ScheduledPayment schedule) {
        return ScheduledPaymentResponse.builder()
                .id(schedule.getId())
                .userId(schedule.getUserId())
                .recipientIdentifier(schedule.getRecipientIdentifier())
                .amount(schedule.getAmount())
                .type(schedule.getType())
                .frequency(schedule.getFrequency())
                .nextExecutionDate(schedule.getNextExecutionDate())
                .lastExecutionDate(schedule.getLastExecutionDate())
                .status(schedule.getStatus())
                .description(schedule.getDescription())
                .totalExecutions(schedule.getTotalExecutions())
                .completedExecutions(schedule.getCompletedExecutions())
                .createdAt(schedule.getCreatedAt())
                .build();
    }
}
