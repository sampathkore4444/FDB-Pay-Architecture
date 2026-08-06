package com.fdbpay.merchant.service.dto.request;

import com.fdbpay.merchant.service.model.enums.RecurringInterval;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringPlanRequest {

    @NotBlank(message = "Plan name is required")
    private String name;

    private String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    @NotBlank(message = "Customer phone is required")
    private String customerPhone;

    private String customerName;

    @NotNull(message = "Interval is required")
    private RecurringInterval interval;

    private Integer dayOfWeek;

    private Integer dayOfMonth;

    private LocalTime time;

    private Integer maxCharges;
}
