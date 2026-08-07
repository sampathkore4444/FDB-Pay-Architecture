package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxSummaryResponse {

    private Long grossRevenue;
    private Long salesTaxCollected;
    private Long withholdingTax;
    private Long netRevenue;
    private double effectiveRatePct;
    private List<TaxInvoiceResponse> invoices;
}
