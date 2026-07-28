package com.fdbpay.wallet.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSavingsPocketRequest {

    @NotBlank(message = "Pocket name is required")
    private String name;

    @NotNull(message = "Goal amount is required")
    @Positive(message = "Goal amount must be positive")
    private Long goalAmount;

    private LocalDate targetDate;
}
