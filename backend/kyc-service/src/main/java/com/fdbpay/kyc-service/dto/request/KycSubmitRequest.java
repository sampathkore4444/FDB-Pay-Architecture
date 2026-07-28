package com.fdbpay.kyc.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycSubmitRequest {

    @NotBlank(message = "KYC tier is required")
    private String tier;

    @NotEmpty(message = "At least one document is required")
    private List<DocumentRequest> documents;
}
