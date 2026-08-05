package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddChargebackNoteRequest {

    @NotBlank(message = "Message is required")
    private String message;

    private String authorName;
}
