package com.emall.reliability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.emall.reliability", "com.emall.common"})
public class ReliabilityApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReliabilityApplication.class, args);
    }
}
