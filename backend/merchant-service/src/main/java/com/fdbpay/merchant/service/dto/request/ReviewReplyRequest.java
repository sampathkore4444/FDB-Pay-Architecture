package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReplyRequest {

    @NotBlank(message = "Reply is required")
    private String reply;
}
