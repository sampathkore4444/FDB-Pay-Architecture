package com.fdbpay.bill.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.fdbpay.bill.service", "com.fdbpay.shared"})
public class BillPaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillPaymentServiceApplication.class, args);
    }
}
