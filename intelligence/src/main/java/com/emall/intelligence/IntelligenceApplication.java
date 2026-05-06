package com.emall.intelligence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.emall.intelligence", "com.emall.common"})
public class IntelligenceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntelligenceApplication.class, args);
    }
}
