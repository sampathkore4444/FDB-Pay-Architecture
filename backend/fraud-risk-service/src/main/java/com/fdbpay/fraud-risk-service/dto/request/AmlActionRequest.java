package com.fdbpay.fraud.risk.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmlActionRequest {

    @NotBlank(message = "Action is required")
    private String action;

    private String reason;
}
