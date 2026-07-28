package com.fdbpay.corporate.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRunResponse {

    private UUID id;
    private UUID corporateUserId;
    private String period;
    private int totalEmployees;
    private Long totalAmount;
    private String status;
    private UUID submittedBy;
    private UUID approvedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
    private List<PayrollEmployeeResponse> employees;
}
