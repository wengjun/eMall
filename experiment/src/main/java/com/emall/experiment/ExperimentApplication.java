package com.emall.experiment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.emall")
public class ExperimentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExperimentApplication.class, args);
    }
}
