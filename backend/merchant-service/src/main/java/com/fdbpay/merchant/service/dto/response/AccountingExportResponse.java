package com.fdbpay.merchant.service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingExportResponse {

    private String accountCode;
    private String description;
    private Long amount;
}
