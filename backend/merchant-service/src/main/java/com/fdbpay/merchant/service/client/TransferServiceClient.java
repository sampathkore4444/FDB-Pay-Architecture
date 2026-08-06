package com.fdbpay.merchant.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class TransferServiceClient {

    private static final String BASE = "http://transfer-service";

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransferServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE).build();
    }

    public boolean charge(UUID merchantUserId, Map<String, Object> chargeRequest) {
        try {
            webClient.post()
                    .uri(uri -> uri.path("/transfer/charge").queryParam("merchantUserId", merchantUserId).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(chargeRequest)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("Recurring charge failed for merchantUserId={}: {}", merchantUserId, e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> getCustomers(UUID walletId) {
        try {
            JsonNode node = webClient.get()
                    .uri(uri -> uri.path("/transfer/analytics/customers").queryParam("walletId", walletId).build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return readDataList(node);
        } catch (Exception e) {
            log.warn("Failed to fetch customer insights for wallet {}: {}", walletId, e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getStorePerformance(UUID walletId) {
        try {
            JsonNode node = webClient.get()
                    .uri(uri -> uri.path("/transfer/analytics/byStore").queryParam("walletId", walletId).build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return readDataList(node);
        } catch (Exception e) {
            log.warn("Failed to fetch store performance for wallet {}: {}", walletId, e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getTransactions(UUID walletId, int size) {
        try {
            JsonNode node = webClient.get()
                    .uri(uri -> uri.path("/transfer/analytics/transactions")
                            .queryParam("walletId", walletId)
                            .queryParam("size", size)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (node != null && node.path("data").path("content").isArray()) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (JsonNode item : node.path("data").path("content")) {
                    result.add(objectMapper.convertValue(item, Map.class));
                }
                return result;
            }
            return List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch transactions for wallet {}: {}", walletId, e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> readDataList(JsonNode node) {
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            if (node != null && node.path("data").isArray()) {
                for (JsonNode item : node.path("data")) {
                    result.add(objectMapper.convertValue(item, Map.class));
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse transfer service response: {}", e.getMessage());
            return List.of();
        }
    }
}
