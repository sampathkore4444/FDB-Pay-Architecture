package com.fdbpay.transfer.service.service;

import com.fdbpay.transfer.service.dto.request.CreateMoneyRequestRequest;
import com.fdbpay.transfer.service.dto.request.RespondMoneyRequestRequest;
import com.fdbpay.transfer.service.dto.response.MoneyRequestResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface MoneyRequestService {

    MoneyRequestResponse create(UUID userId, CreateMoneyRequestRequest request);

    MoneyRequestResponse respond(UUID requestId, RespondMoneyRequestRequest response, UUID targetUserId);

    Page<MoneyRequestResponse> getMyRequests(UUID userId, int page, int size);

    Page<MoneyRequestResponse> getByPhone(String phone, int page, int size);
}
