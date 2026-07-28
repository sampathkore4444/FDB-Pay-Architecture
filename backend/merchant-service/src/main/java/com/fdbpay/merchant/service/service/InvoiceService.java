package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.CreateInvoiceRequest;
import com.fdbpay.merchant.service.dto.response.InvoiceResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface InvoiceService {

    InvoiceResponse create(UUID merchantId, CreateInvoiceRequest request);

    InvoiceResponse send(UUID invoiceId);

    InvoiceResponse markPaid(UUID invoiceId);

    InvoiceResponse cancel(UUID invoiceId);

    Page<InvoiceResponse> getByMerchant(UUID merchantId, int page, int size);
}
