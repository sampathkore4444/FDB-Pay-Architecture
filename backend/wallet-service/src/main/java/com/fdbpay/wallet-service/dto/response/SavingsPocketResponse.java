package com.fdbpay.wallet.service.dto.response;

import com.fdbpay.wallet.service.model.enums.PocketStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsPocketResponse {

    private UUID id;
    private String name;
    private Long goalAmount;
    private Long currentAmount;
    private double progressPercentage;
    private BigDecimal interestRate;
    private PocketStatus status;
    private LocalDate targetDate;
    private OffsetDateTime createdAt;
}
