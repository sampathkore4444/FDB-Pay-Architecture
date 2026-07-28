package com.fdbpay.support.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddMessageRequest {

    @NotBlank(message = "Message is required")
    private String message;

    private String attachments;
}
