package com.sliit.vehiclebiddingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Enables @Scheduled tasks
public class VehicleBiddingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(VehicleBiddingSystemApplication.class, args);
    }
}