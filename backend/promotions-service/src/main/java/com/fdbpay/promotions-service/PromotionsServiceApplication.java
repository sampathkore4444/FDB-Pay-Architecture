package com.fdbpay.promotions.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.fdbpay.promotions.service", "com.fdbpay.shared"})
@EnableDiscoveryClient
public class PromotionsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromotionsServiceApplication.class, args);
    }
}
