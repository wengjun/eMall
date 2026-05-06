package com.emall.cost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.emall")
public class CostApplication {
    public static void main(String[] args) {
        SpringApplication.run(CostApplication.class, args);
    }
}
