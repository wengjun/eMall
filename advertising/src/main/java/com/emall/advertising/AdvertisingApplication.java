package com.emall.advertising;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.emall")
public class AdvertisingApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdvertisingApplication.class, args);
    }
}
