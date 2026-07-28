package com.fdbpay.kyc.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {"com.fdbpay.kyc.service", "com.fdbpay.shared"})
@EnableMongoRepositories(basePackages = "com.fdbpay.kyc.service.repository")
public class KycServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KycServiceApplication.class, args);
    }
}
