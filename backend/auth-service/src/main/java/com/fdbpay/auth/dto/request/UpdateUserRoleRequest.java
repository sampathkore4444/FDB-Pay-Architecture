package com.fdbpay.auth.dto.request;

import com.fdbpay.auth.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRoleRequest {

    @NotNull(message = "Role is required")
    private UserRole role;
}
