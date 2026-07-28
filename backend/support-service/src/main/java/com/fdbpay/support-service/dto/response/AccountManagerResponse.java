package com.fdbpay.support.service.dto.response;

import com.fdbpay.support.service.model.AccountManager;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountManagerResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private Integer maxClients;
    private Integer currentClients;
    private AccountManager.AccountManagerStatus status;
    private OffsetDateTime createdAt;
}
