package com.emall.forecasting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.emall.forecasting", "com.emall.common"})
public class ForecastingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ForecastingApplication.class, args);
    }
}
