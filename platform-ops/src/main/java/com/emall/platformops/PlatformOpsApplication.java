package com.emall.platformops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.emall")
public class PlatformOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformOpsApplication.class, args);
    }
}
