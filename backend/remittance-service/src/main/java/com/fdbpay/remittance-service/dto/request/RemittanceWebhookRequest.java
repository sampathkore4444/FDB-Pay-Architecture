package com.fdbpay.remittance.service.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemittanceWebhookRequest {

    private String partnerRef;
    private String status;
    private String referenceNumber;
}
