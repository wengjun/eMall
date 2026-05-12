package com.emall.pricing;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDubbo(scanBasePackages = "com.emall.pricing.rpc")
@SpringBootApplication
public class PricingApplication {
    public static void main(String[] args) {
        SpringApplication.run(PricingApplication.class, args);
    }
}
