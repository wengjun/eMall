package com.emall.operations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.emall")
public class OperationsApplication {
    public static void main(String[] args) {
        SpringApplication.run(OperationsApplication.class, args);
    }
}
