package com.fdbpay.corporate.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitPayrollRequest {

    @NotNull(message = "Payroll run ID is required")
    private UUID payrollRunId;
}
