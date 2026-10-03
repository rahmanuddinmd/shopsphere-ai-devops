package com.shopsphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * ShopSphere AI - Spring Boot entry point.
 * Modules (packages): security/auth, product, cart, order, admin, ai.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ShopSphereApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopSphereApplication.class, args);
    }
}
