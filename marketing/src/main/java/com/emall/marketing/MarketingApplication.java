package com.emall.marketing;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDubbo(scanBasePackages = "com.emall.marketing.rpc")
@SpringBootApplication
public class MarketingApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketingApplication.class, args);
    }
}
