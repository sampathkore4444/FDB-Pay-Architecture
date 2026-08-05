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

    public Map<String, String> getUser(UUID userId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.get()
                    .uri("/internal/users/{id}", userId)
                    .retrieve()
                    .body(Map.class);
            if (body == null || body.get("data") == null) {
                return Map.of();
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            return Map.of(
                    "name", String.valueOf(data.getOrDefault("name", "")),
                    "phone", String.valueOf(data.getOrDefault("phone", "")));
        } catch (Exception e) {
            log.warn("Failed to fetch user {} for enrichment: {}", userId, e.getMessage());
            return Map.of();
        }
    }
}
