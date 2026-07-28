package com.fdbpay.corporate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePayrollRunRequest {

    @NotBlank(message = "Period is required")
    private String period;

    @NotEmpty(message = "Employee list cannot be empty")
    private List<PayrollEmployeeItem> employees;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PayrollEmployeeItem {

        @NotBlank(message = "Employee ID is required")
        private String employeeId;

        @NotBlank(message = "Employee name is required")
        private String employeeName;

        @NotBlank(message = "Phone is required")
        private String phone;

        @NotBlank(message = "Amount is required")
        private Long amount;
    }
}
