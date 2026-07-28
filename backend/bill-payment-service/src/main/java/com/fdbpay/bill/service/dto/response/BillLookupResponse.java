package com.fdbpay.bill.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillLookupResponse {

    private String accountNumber;
    private String accountName;
    private Long amountDue;
    private OffsetDateTime dueDate;
    private String billerName;
    private String period;
}
