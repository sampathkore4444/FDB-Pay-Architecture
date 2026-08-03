package com.fdbpay.merchant.service.client;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class AuthServiceClient {

    private final RestClient restClient;

    public AuthServiceClient(@Value("${auth-service.base-url:http://localhost:8081}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void upgradeUserRole(UUID userId, String role) {
        try {
            restClient.put()
                    .uri("/internal/users/{id}/role", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("role", role))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to upgrade role for user {} to {}: {}", userId, role, e.getMessage());
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR,
                    "Failed to upgrade user role after merchant approval");
        }
    }
}
