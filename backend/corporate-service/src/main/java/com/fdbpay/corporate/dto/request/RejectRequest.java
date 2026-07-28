package com.fdbpay.corporate.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectRequest {

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
