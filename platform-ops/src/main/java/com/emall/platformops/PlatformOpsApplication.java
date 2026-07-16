package com.emall.platformops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.emall")
@EnableScheduling
public class PlatformOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformOpsApplication.class, args);
    }
}
