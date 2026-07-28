package com.fdbpay.bill.service.service;

import com.fdbpay.bill.service.dto.request.AirtimeTopupRequest;
import com.fdbpay.bill.service.dto.response.AirtimeTopupResponse;
import com.fdbpay.bill.service.model.enums.AirtimeProvider;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface AirtimeTopupService {

    AirtimeTopupResponse topup(UUID userId, AirtimeTopupRequest request);

    Page<AirtimeTopupResponse> getHistory(UUID userId, int page, int size);

    List<AirtimeProvider> getProviders();
}
