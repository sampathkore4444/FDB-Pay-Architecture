package com.fdbpay.schemas.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phone;

    private String name;

    private String email;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "PIN must be 4-6 digits")
    private String pin;
}
