package com.fdbpay.corporate.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollEmployeeResponse {

    private UUID id;
    private UUID payrollRunId;
    private String employeeId;
    private String employeeName;
    private String phone;
    private Long amount;
    private String status;
    private String transactionRef;
}
