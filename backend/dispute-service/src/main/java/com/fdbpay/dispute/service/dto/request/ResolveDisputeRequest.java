package com.fdbpay.dispute.service.dto.request;

import com.fdbpay.dispute.service.model.enums.DisputeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolveDisputeRequest {

    @NotNull(message = "status is required")
    private DisputeStatus status;

    private String resolution;
}
