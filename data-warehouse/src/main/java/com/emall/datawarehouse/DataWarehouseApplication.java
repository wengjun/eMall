package com.emall.datawarehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.emall.datawarehouse", "com.emall.common"})
public class DataWarehouseApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataWarehouseApplication.class, args);
    }
}
