package com.fdbpay.referencedata.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReferenceValueRequest {

    @NotBlank(message = "Value is required")
    @Size(max = 255, message = "Value must be at most 255 characters")
    private String value;

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must be at most 50 characters")
    private String code;

    private Integer sortOrder;

    private Boolean active;
}
