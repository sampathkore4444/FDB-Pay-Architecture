package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.client.TransferServiceClient;
import com.fdbpay.merchant.service.dto.request.RecurringPlanRequest;
import com.fdbpay.merchant.service.dto.response.RecurringPlanResponse;
import com.fdbpay.merchant.service.model.RecurringPlan;
import com.fdbpay.merchant.service.model.enums.RecurringInterval;
import com.fdbpay.merchant.service.model.enums.RecurringPlanStatus;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.RecurringPlanRepository;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringBillingService {

    private final RecurringPlanRepository planRepository;
    private final MerchantRepository merchantRepository;
    private final TransferServiceClient transferServiceClient;
    private final AuditService auditService;

    public List<RecurringPlanResponse> list(UUID merchantId) {
        requireMerchant(merchantId);
        return planRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public RecurringPlanResponse create(UUID merchantId, RecurringPlanRequest request) {
        requireMerchant(merchantId);
        RecurringPlan plan = RecurringPlan.builder()
                .merchantId(merchantId)
                .name(request.getName())
                .description(request.getDescription())
                .amount(request.getAmount())
                .customerPhone(request.getCustomerPhone())
                .customerName(request.getCustomerName())
                .interval(request.getInterval())
                .dayOfWeek(request.getDayOfWeek())
                .dayOfMonth(request.getDayOfMonth())
                .status(RecurringPlanStatus.ACTIVE)
                .maxCharges(request.getMaxCharges())
                .chargeCount(0)
                .nextRunAt(computeNextRun(request, LocalTime.now()))
                .build();
        plan = planRepository.save(plan);
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "RECURRING_PLAN",
                plan.getId().toString(), "Created recurring plan '" + plan.getName() + "' for " + plan.getCustomerPhone());
        return mapToResponse(plan);
    }

    @Transactional
    public RecurringPlanResponse update(UUID merchantId, UUID planId, RecurringPlanRequest request) {
        RecurringPlan plan = getOwned(merchantId, planId);
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setAmount(request.getAmount());
        plan.setCustomerPhone(request.getCustomerPhone());
        plan.setCustomerName(request.getCustomerName());
        plan.setInterval(request.getInterval());
        plan.setDayOfWeek(request.getDayOfWeek());
        plan.setDayOfMonth(request.getDayOfMonth());
        plan.setMaxCharges(request.getMaxCharges());
        if (plan.getStatus() == RecurringPlanStatus.ACTIVE) {
            plan.setNextRunAt(computeNextRun(request, plan.getNextRunAt() == null ? LocalTime.now() : plan.getNextRunAt().toLocalTime()));
        }
        plan = planRepository.save(plan);
        auditService.log(merchantId, "OWNER", null, null, "UPDATE", "RECURRING_PLAN",
                plan.getId().toString(), "Updated recurring plan '" + plan.getName() + "'");
        return mapToResponse(plan);
    }

    @Transactional
    public RecurringPlanResponse setStatus(UUID merchantId, UUID planId, RecurringPlanStatus status) {
        RecurringPlan plan = getOwned(merchantId, planId);
        plan.setStatus(status);
        if (status == RecurringPlanStatus.ACTIVE) {
            plan.setNextRunAt(computeNextRun(plan, LocalTime.now()));
        }
        plan = planRepository.save(plan);
        auditService.log(merchantId, "OWNER", null, null, "STATUS_CHANGE", "RECURRING_PLAN",
                plan.getId().toString(), "Recurring plan status -> " + status);
        return mapToResponse(plan);
    }

    @Transactional
    public void delete(UUID merchantId, UUID planId) {
        RecurringPlan plan = getOwned(merchantId, planId);
        planRepository.delete(plan);
        auditService.log(merchantId, "OWNER", null, null, "DELETE", "RECURRING_PLAN", planId.toString(), "Deleted recurring plan");
    }

    @Transactional
    public RecurringPlanResponse runNow(UUID merchantId, UUID planId) {
        RecurringPlan plan = getOwned(merchantId, planId);
        execute(plan);
        return mapToResponse(plan);
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processDuePlans() {
        OffsetDateTime now = OffsetDateTime.now();
        List<RecurringPlan> due = planRepository.findByStatusAndNextRunAtLessThanEqual(RecurringPlanStatus.ACTIVE, now);
        for (RecurringPlan plan : due) {
            try {
                execute(plan);
            } catch (Exception e) {
                log.warn("Scheduled recurring charge failed for plan {}: {}", plan.getId(), e.getMessage());
                plan.setStatus(RecurringPlanStatus.PAUSED);
                planRepository.save(plan);
            }
        }
    }

    private void execute(RecurringPlan plan) {
        Map<String, Object> charge = Map.of(
                "customerPhone", plan.getCustomerPhone(),
                "customerName", plan.getCustomerName() == null ? "" : plan.getCustomerName(),
                "cardLast4", "8888",
                "amount", plan.getAmount(),
                "description", "Recurring: " + plan.getName(),
                "idempotencyKey", "recurring_" + plan.getId() + "_" + System.currentTimeMillis());

        UUID merchantUserId = merchantRepository.findById(plan.getMerchantId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", plan.getMerchantId().toString()))
                .getUserId();

        if (!transferServiceClient.charge(merchantUserId, charge)) {
            plan.setStatus(RecurringPlanStatus.PAUSED);
            planRepository.save(plan);
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR, "Recurring charge failed for plan " + plan.getId());
        }

        plan.setChargeCount(plan.getChargeCount() + 1);
        plan.setLastChargeAt(OffsetDateTime.now());
        if (plan.getMaxCharges() != null && plan.getChargeCount() >= plan.getMaxCharges()) {
            plan.setStatus(RecurringPlanStatus.COMPLETED);
            plan.setNextRunAt(null);
        } else {
            plan.setNextRunAt(computeNextRun(plan, LocalTime.now()));
        }
        planRepository.save(plan);
        auditService.log(plan.getMerchantId(), "SYSTEM", "Recurring Billing", null, "CHARGE",
                "RECURRING_PLAN", plan.getId().toString(),
                "Charged " + plan.getAmount() + " MMK to " + plan.getCustomerPhone() + " (" + (plan.getChargeCount()) + " charges)");
    }

    private OffsetDateTime computeNextRun(RecurringPlanRequest request, LocalTime time) {
        OffsetDateTime now = OffsetDateTime.now();
        if (request.getInterval() == RecurringInterval.WEEKLY && request.getDayOfWeek() != null) {
            DayOfWeek dow = DayOfWeek.of(Math.max(1, Math.min(7, request.getDayOfWeek())));
            OffsetDateTime next = now.with(TemporalAdjusters.nextOrSame(dow)).withHour(time.getHour()).withMinute(time.getMinute()).withSecond(0).withNano(0);
            if (!next.isAfter(now)) {
                next = next.plusWeeks(1);
            }
            return next;
        }
        int dom = request.getDayOfMonth() == null ? 1 : Math.max(1, Math.min(28, request.getDayOfMonth()));
        OffsetDateTime next = now.withDayOfMonth(dom).withHour(time.getHour()).withMinute(time.getMinute()).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusMonths(1);
        }
        return next;
    }

    private OffsetDateTime computeNextRun(RecurringPlan plan, LocalTime time) {
        RecurringPlanRequest request = RecurringPlanRequest.builder()
                .interval(plan.getInterval())
                .dayOfWeek(plan.getDayOfWeek())
                .dayOfMonth(plan.getDayOfMonth())
                .build();
        return computeNextRun(request, time);
    }

    private RecurringPlan getOwned(UUID merchantId, UUID planId) {
        RecurringPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("RecurringPlan", planId.toString()));
        if (!plan.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Plan does not belong to this merchant");
        }
        return plan;
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private RecurringPlanResponse mapToResponse(RecurringPlan plan) {
        return RecurringPlanResponse.builder()
                .id(plan.getId())
                .merchantId(plan.getMerchantId())
                .name(plan.getName())
                .description(plan.getDescription())
                .amount(plan.getAmount())
                .customerPhone(plan.getCustomerPhone())
                .customerName(plan.getCustomerName())
                .interval(plan.getInterval())
                .dayOfWeek(plan.getDayOfWeek())
                .dayOfMonth(plan.getDayOfMonth())
                .status(plan.getStatus())
                .maxCharges(plan.getMaxCharges())
                .chargeCount(plan.getChargeCount())
                .nextRunAt(plan.getNextRunAt())
                .lastChargeAt(plan.getLastChargeAt())
                .createdAt(plan.getCreatedAt())
                .build();
    }
}
