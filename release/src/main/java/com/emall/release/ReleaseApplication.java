package com.emall.release;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.emall")
@EnableScheduling
public class ReleaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReleaseApplication.class, args);
    }
}
