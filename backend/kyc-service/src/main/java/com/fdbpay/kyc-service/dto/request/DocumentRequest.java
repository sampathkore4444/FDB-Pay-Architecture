package com.fdbpay.kyc.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentRequest {

    @NotBlank(message = "Document type is required")
    private String type;

    @NotBlank(message = "File URL is required")
    private String fileUrl;
}
