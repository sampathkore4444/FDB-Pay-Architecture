package com.fdbpay.kyc.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminKycReviewRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String reason;
}
