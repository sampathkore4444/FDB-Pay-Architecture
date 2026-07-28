package com.fdbpay.schemas.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "PIN must be 4-6 digits")
    private String pin;

    private String deviceId;
}
