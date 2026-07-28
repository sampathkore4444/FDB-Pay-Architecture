package com.fdbpay.fraud.risk.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.fdbpay.fraud.risk.service", "com.fdbpay.shared"})
public class FraudRiskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudRiskServiceApplication.class, args);
    }
}
