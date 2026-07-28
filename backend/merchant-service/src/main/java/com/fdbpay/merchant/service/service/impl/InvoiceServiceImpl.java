package com.fdbpay.merchant.service.service.impl;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.event.NotificationEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.merchant.service.dto.request.CreateInvoiceRequest;
import com.fdbpay.merchant.service.dto.response.InvoiceResponse;
import com.fdbpay.merchant.service.model.Invoice;
import com.fdbpay.merchant.service.model.enums.InvoiceStatus;
import com.fdbpay.merchant.service.repository.InvoiceRepository;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final MerchantRepository merchantRepository;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    private static final String NOTIFICATION_TOPIC = "notification.send";

    @Override
    @Transactional
    public InvoiceResponse create(UUID merchantId, CreateInvoiceRequest request) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        Invoice invoice = Invoice.builder()
                .merchantId(merchantId)
                .customerPhone(request.getCustomerPhone())
                .customerName(request.getCustomerName())
                .items(request.getItems())
                .subtotal(request.getSubtotal())
                .tax(request.getTax())
                .total(request.getTotal())
                .status(InvoiceStatus.DRAFT)
                .dueDate(request.getDueDate())
                .build();

        invoice = invoiceRepository.save(invoice);
        log.info("Invoice created: invoiceId={}, merchantId={}, total={}",
                invoice.getId(), merchantId, request.getTotal());

        return mapToResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse send(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId.toString()));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Only DRAFT invoices can be sent. Current status: " + invoice.getStatus());
        }

        invoice.setStatus(InvoiceStatus.SENT);
        invoice = invoiceRepository.save(invoice);

        sendInvoiceNotification(invoice);

        log.info("Invoice sent: invoiceId={}, customerPhone={}", invoiceId, invoice.getCustomerPhone());

        return mapToResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse markPaid(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId.toString()));

        if (invoice.getStatus() != InvoiceStatus.SENT) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Only SENT invoices can be marked as paid. Current status: " + invoice.getStatus());
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(OffsetDateTime.now());
        invoice = invoiceRepository.save(invoice);

        log.info("Invoice marked as paid: invoiceId={}", invoiceId);

        return mapToResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse cancel(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId.toString()));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Cannot cancel a paid invoice");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice = invoiceRepository.save(invoice);

        log.info("Invoice cancelled: invoiceId={}", invoiceId);

        return mapToResponse(invoice);
    }

    @Override
    public Page<InvoiceResponse> getByMerchant(UUID merchantId, int page, int size) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Invoice> invoices = invoiceRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageRequest);
        return invoices.map(this::mapToResponse);
    }

    private void sendInvoiceNotification(Invoice invoice) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .userId(invoice.getMerchantId())
                    .channel("SMS")
                    .type("INVOICE_SENT")
                    .title("Invoice Sent")
                    .body(String.format("Invoice of %d MMK has been sent to %s",
                            invoice.getTotal(), invoice.getCustomerPhone()))
                    .phone(invoice.getCustomerPhone())
                    .timestamp(OffsetDateTime.now())
                    .build();
            kafkaTemplate.send(NOTIFICATION_TOPIC, invoice.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to send invoice notification: invoiceId={}", invoice.getId(), e);
        }
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .merchantId(invoice.getMerchantId())
                .customerPhone(invoice.getCustomerPhone())
                .customerName(invoice.getCustomerName())
                .items(invoice.getItems())
                .subtotal(invoice.getSubtotal())
                .tax(invoice.getTax())
                .total(invoice.getTotal())
                .status(invoice.getStatus())
                .dueDate(invoice.getDueDate())
                .paidAt(invoice.getPaidAt())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
