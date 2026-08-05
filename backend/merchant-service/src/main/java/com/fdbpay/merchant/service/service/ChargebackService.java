package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.AddChargebackNoteRequest;
import com.fdbpay.merchant.service.dto.request.AddChargebackRequest;
import com.fdbpay.merchant.service.dto.request.RespondChargebackRequest;
import com.fdbpay.merchant.service.dto.response.ChargebackResponse;

import java.util.List;
import java.util.UUID;

public interface ChargebackService {

    List<ChargebackResponse> getByMerchant(UUID merchantId);

    ChargebackResponse getDetail(UUID merchantId, UUID chargebackId);

    ChargebackResponse openChargeback(UUID merchantId, AddChargebackRequest request);

    ChargebackResponse addNote(UUID merchantId, UUID chargebackId, AddChargebackNoteRequest request);

    ChargebackResponse respond(UUID merchantId, UUID chargebackId, RespondChargebackRequest request);
}
