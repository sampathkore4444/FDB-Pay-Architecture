package com.fdbpay.remittance.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.fdbpay.remittance.service", "com.fdbpay.shared"})
@EnableDiscoveryClient
public class RemittanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RemittanceServiceApplication.class, args);
    }
}
