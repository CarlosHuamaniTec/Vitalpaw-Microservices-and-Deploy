package com.vitalpaw.sensordataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.vitalpaw.sensordataservice")
public class SensordataApplication {
    public static void main(String[] args) {
        SpringApplication.run(SensordataApplication.class, args);
    }
}