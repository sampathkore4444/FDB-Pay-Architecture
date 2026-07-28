package com.fdbpay.fraud.risk.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanctionScreeningRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "NRC number is required")
    private String nrcNumber;

    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth;
}
