package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyRequest {

    @NotBlank(message = "Key name is required")
    private String name;
}
