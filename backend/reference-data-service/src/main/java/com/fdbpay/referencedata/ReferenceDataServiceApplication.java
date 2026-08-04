package com.fdbpay.referencedata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.fdbpay.referencedata", "com.fdbpay.shared"})
@EnableDiscoveryClient
public class ReferenceDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReferenceDataServiceApplication.class, args);
    }
}
