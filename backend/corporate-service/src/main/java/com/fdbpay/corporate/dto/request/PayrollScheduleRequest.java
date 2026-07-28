package com.fdbpay.corporate.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollScheduleRequest {

    @NotNull(message = "Scheduled date is required")
    private LocalDate scheduledDate;
}
