package com.fdbpay.schemas.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PinSetRequest {

    @NotBlank(message = "Current PIN is required")
    private String currentPin;

    @NotBlank(message = "New PIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "PIN must be 4-6 digits")
    private String newPin;
}
