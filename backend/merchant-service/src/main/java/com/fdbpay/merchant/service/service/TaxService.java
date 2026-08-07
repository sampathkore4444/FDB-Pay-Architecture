package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.CreateTaxInvoiceRequest;
import com.fdbpay.merchant.service.dto.response.TaxInvoiceResponse;
import com.fdbpay.merchant.service.dto.response.TaxSummaryResponse;
import com.fdbpay.merchant.service.model.TaxInvoice;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.OrderRepository;
import com.fdbpay.merchant.service.repository.TaxInvoiceRepository;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxService {

    private final TaxInvoiceRepository taxInvoiceRepository;
    private final MerchantRepository merchantRepository;

    public List<TaxInvoiceResponse> listInvoices(UUID merchantId) {
        requireMerchant(merchantId);
        return taxInvoiceRepository.findByMerchantIdOrderByIssueDateDesc(merchantId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public TaxInvoiceResponse createInvoice(UUID merchantId, CreateTaxInvoiceRequest request) {
        requireMerchant(merchantId);
        long tax = request.getTax() == null ? 0L : request.getTax();
        long withholding = request.getWithholdingTax() == null ? 0L : request.getWithholdingTax();
        long total = request.getSubtotal() + tax;
        LocalDate issueDate = request.getIssueDate() == null ? LocalDate.now() : request.getIssueDate();
        String invoiceNo = "TINV-" + issueDate.format(DateTimeFormatter.ofPattern("yyyyMM")) + "-"
                + String.format("%04d", taxInvoiceRepository.findByMerchantIdOrderByIssueDateDesc(merchantId).size() + 1);
        TaxInvoice invoice = TaxInvoice.builder()
                .merchantId(merchantId)
                .invoiceNo(invoiceNo)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .subtotal(request.getSubtotal())
                .tax(tax)
                .withholdingTax(withholding)
                .total(total)
                .issueDate(issueDate)
                .build();
        invoice = taxInvoiceRepository.save(invoice);
        return mapToResponse(invoice);
    }

    public TaxSummaryResponse summary(UUID merchantId, LocalDate from, LocalDate to) {
        requireMerchant(merchantId);
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        Long tax = taxInvoiceRepository.sumTax(merchantId, start, end);
        Long withholding = taxInvoiceRepository.sumWithholding(merchantId, start, end);
        Long total = taxInvoiceRepository.sumTotal(merchantId, start, end);
        long subtotal = total - tax;
        return TaxSummaryResponse.builder()
                .grossRevenue(total)
                .salesTaxCollected(tax)
                .withholdingTax(withholding)
                .netRevenue(subtotal - withholding)
                .effectiveRatePct(subtotal == 0 ? 0 : Math.round(tax * 100.0 / subtotal))
                .invoices(taxInvoiceRepository.findByMerchantIdAndIssueDateBetweenOrderByIssueDateDesc(merchantId, start, end)
                        .stream().map(this::mapToResponse).toList())
                .build();
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private TaxInvoiceResponse mapToResponse(TaxInvoice invoice) {
        return TaxInvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNo(invoice.getInvoiceNo())
                .customerName(invoice.getCustomerName())
                .customerPhone(invoice.getCustomerPhone())
                .subtotal(invoice.getSubtotal())
                .tax(invoice.getTax())
                .withholdingTax(invoice.getWithholdingTax())
                .total(invoice.getTotal())
                .issueDate(invoice.getIssueDate())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
