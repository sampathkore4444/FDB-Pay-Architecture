package com.fdbpay.reporting.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${services.wallet-service.base-url}")
    private String walletServiceBaseUrl;

    @Value("${services.transfer-service.base-url}")
    private String transferServiceBaseUrl;

    @Bean(name = "walletWebClient")
    public WebClient walletWebClient() {
        return WebClient.builder()
                .baseUrl(walletServiceBaseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    @Bean(name = "transferWebClient")
    public WebClient transferWebClient() {
        return WebClient.builder()
                .baseUrl(transferServiceBaseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }
}
