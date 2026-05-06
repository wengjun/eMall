package com.emall.eventplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.emall.eventplatform", "com.emall.common"})
public class EventPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventPlatformApplication.class, args);
    }
}
