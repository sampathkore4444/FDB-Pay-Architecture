package com.fdbpay.corporate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${wallet-service.base-url:http://localhost:8082}")
    private String walletServiceBaseUrl;

    @Bean(name = "walletWebClient")
    public WebClient walletWebClient() {
        return WebClient.builder()
                .baseUrl(walletServiceBaseUrl)
                .build();
    }
}
