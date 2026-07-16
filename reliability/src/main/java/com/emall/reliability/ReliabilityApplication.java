package com.emall.reliability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.emall.reliability", "com.emall.common"})
@EnableScheduling
public class ReliabilityApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReliabilityApplication.class, args);
    }
}
