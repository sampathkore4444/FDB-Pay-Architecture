package com.fdbpay.merchant.service.dto.request;

import com.fdbpay.merchant.service.model.enums.PosTerminalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePosTerminalRequest {

    @NotBlank(message = "Serial number is required")
    private String serialNumber;

    @NotNull(message = "Terminal type is required")
    private PosTerminalType type;
}
