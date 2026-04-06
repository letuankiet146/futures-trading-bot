package com.trading.simulate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SimulateServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SimulateServiceApplication.class, args);
    }
}
