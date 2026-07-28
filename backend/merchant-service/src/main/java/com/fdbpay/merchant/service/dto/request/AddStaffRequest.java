package com.fdbpay.merchant.service.dto.request;

import com.fdbpay.merchant.service.model.enums.StaffRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddStaffRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Role is required")
    private StaffRole role;

    private Long dailyLimit;
}
