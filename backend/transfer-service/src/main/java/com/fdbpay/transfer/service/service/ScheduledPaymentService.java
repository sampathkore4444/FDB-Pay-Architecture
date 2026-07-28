package com.fdbpay.transfer.service.service;

import com.fdbpay.transfer.service.dto.request.CreateScheduledPaymentRequest;
import com.fdbpay.transfer.service.dto.response.ScheduledPaymentResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ScheduledPaymentService {

    ScheduledPaymentResponse create(UUID userId, CreateScheduledPaymentRequest request);

    Page<ScheduledPaymentResponse> getMySchedules(UUID userId, int page, int size);

    ScheduledPaymentResponse cancel(UUID scheduleId, UUID userId);

    ScheduledPaymentResponse pause(UUID scheduleId, UUID userId);

    ScheduledPaymentResponse resume(UUID scheduleId, UUID userId);

    void executeDueSchedules();
}
