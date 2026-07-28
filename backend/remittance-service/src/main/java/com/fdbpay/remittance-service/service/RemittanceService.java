package com.fdbpay.remittance.service.service;

import com.fdbpay.remittance.service.dto.request.InitiateRemittanceRequest;
import com.fdbpay.remittance.service.dto.request.RemittanceWebhookRequest;
import com.fdbpay.remittance.service.dto.response.RemittanceCorridorResponse;
import com.fdbpay.remittance.service.dto.response.RemittanceRateQuoteResponse;
import com.fdbpay.remittance.service.dto.response.RemittanceResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface RemittanceService {

    List<RemittanceCorridorResponse> getCorridors();

    RemittanceRateQuoteResponse getRateQuote(String corridorCode, Long amount);

    RemittanceResponse initiateRemittance(UUID userId, InitiateRemittanceRequest request);

    RemittanceResponse getRemittance(UUID id);

    Page<RemittanceResponse> getMyRemittances(UUID userId, int page, int size);

    RemittanceResponse processWebhook(RemittanceWebhookRequest request);

    RemittanceResponse handleCallback(RemittanceWebhookRequest request);
}
