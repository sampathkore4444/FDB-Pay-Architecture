package com.fdbpay.transfer.service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespondMoneyRequestRequest {

    @NotNull(message = "Action is required")
    private Action action;

    public enum Action {
        ACCEPT,
        CANCEL
    }
}
