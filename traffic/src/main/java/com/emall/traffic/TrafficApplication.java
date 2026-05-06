package com.emall.traffic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.emall.traffic", "com.emall.common"})
public class TrafficApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrafficApplication.class, args);
    }
}
