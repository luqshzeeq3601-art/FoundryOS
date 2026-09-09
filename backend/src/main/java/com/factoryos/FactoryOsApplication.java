package com.factoryos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class FactoryOsApplication {

    public static void main(String[] args) {
        // Enforce UTC JVM timezone globally
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(FactoryOsApplication.class, args);
    }
}
