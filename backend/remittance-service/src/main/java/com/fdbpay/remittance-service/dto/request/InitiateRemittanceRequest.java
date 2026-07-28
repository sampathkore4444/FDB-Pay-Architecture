package com.fdbpay.remittance.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiateRemittanceRequest {

    @NotBlank(message = "Recipient phone is required")
    private String recipientPhone;

    @NotBlank(message = "Sender name is required")
    private String senderName;

    @NotBlank(message = "Sender country is required")
    private String senderCountry;

    @NotBlank(message = "Corridor is required")
    private String corridor;

    @Positive(message = "Amount must be positive")
    private Long amount;

    private String partnerRef;
}
